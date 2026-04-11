package com.aiagent.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;

/**
 * 异步任务配置类
 * <p>
 * 配置 Spring MVC 异步请求支持和自定义线程池执行器：
 * <ul>
 *   <li>evaluationTaskExecutor - 评估任务专用线程池，核心线程数 2，最大线程数 5</li>
 *   <li>taskExecutor - MVC 异步请求默认线程池，核心线程数 10，最大线程数 50</li>
 * </ul>
 * </p>
 *
 * @see WebMvcConfigurer
 */
@Configuration
@EnableAsync
public class AsyncConfig implements WebMvcConfigurer {

    /**
     * 创建评估任务专用线程池执行器
     * <p>
     * 用于执行评估任务（如批量模型评估），配置较小的线程池以避免资源占用过高：
     * <ul>
     *   <li>核心线程数：2</li>
     *   <li>最大线程数：5</li>
     *   <li>队列容量：100</li>
     * </ul>
     * </p>
     *
     * @return 配置好的 ThreadPoolTaskExecutor 实例
     */
    @Bean("evaluationTaskExecutor")
    public Executor evaluationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("evaluation-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 配置 Spring MVC 异步请求的默认 TaskExecutor
     * <p>
     * 用于替代 SimpleAsyncTaskExecutor，避免在生产环境下创建过多线程。
     * 配置参数：
     * <ul>
     *   <li>核心线程数：10</li>
     *   <li>最大线程数：50</li>
     *   <li>队列容量：200</li>
     *   <li>拒绝策略：CallerRunsPolicy（由调用线程执行）</li>
     * </ul>
     * </p>
     *
     * @return 配置好的 ThreadPoolTaskExecutor 实例
     */
    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("mvc-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 配置 Spring MVC 异步支持
     * <p>
     * 解决警告: "Performing asynchronous handling through the default Spring MVC SimpleAsyncTaskExecutor"
     * 设置默认超时时间为 5 分钟（300000ms），适用于流式响应等长时间异步操作。
     * </p>
     *
     * @param configurer 异步支持配置器
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(taskExecutor());
        // 设置异步请求超时时间（5分钟）
        configurer.setDefaultTimeout(300000);
    }
}
