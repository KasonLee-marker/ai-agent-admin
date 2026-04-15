package com.aiagent.admin.service.event;

import com.aiagent.admin.service.impl.DocumentAsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档处理事件监听器
 * <p>
 * 监听文档上传和 Embedding 开始事件，在事务提交后异步执行处理任务。
 * 使用 {@link TransactionalEventListener} 确保只有在事务成功提交后才执行异步任务。
 * </p>
 *
 * <p><b>为什么使用 TransactionalEventListener？</b></p>
 * <ul>
 *   <li>确保异步任务只在事务成功提交后执行</li>
 *   <li>避免异步线程读取未提交数据的问题</li>
 *   <li>Spring 事务基础设施自动管理事件发布和监听</li>
 * </ul>
 *
 * <p><b>为什么使用 byte[] 而不是 MultipartFile？</b></p>
 * <ul>
 *   <li>HTTP 请求结束后 Tomcat 会清理临时上传文件</li>
 *   <li>异步线程执行时临时文件可能已不存在</li>
 *   <li>使用 byte[] 存储文件内容，在事件发布前读取</li>
 *   <li>在异步线程中创建 MockMultipartFile 进行处理</li>
 * </ul>
 *
 * @see DocumentUploadEvent
 * @see EmbeddingStartEvent
 * @see DocumentAsyncService
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingEventListener {

    private final DocumentAsyncService documentAsyncService;

    /**
     * 处理文档上传事件
     * <p>
     * 在事务提交后异步执行文档处理（提取文本、分块）。
     * 使用 MockMultipartFile 包装文件字节数组。
     * </p>
     *
     * @param event 文档上传事件
     */
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentUpload(DocumentUploadEvent event) {
        log.info("Received DocumentUploadEvent after transaction commit, processing document: {}", event.getDocumentId());

        // 使用 MockMultipartFile 包装字节数组（因为原始临时文件可能已被清理）
        MultipartFile mockFile = new MockMultipartFile(
                "file",
                event.getOriginalFilename(),
                event.getContentType(),
                event.getFileContent()
        );

        documentAsyncService.processDocumentAsync(
                event.getDocumentId(),
                mockFile,
                event.getContentType(),
                event.getEmbeddingModelId()
        );
    }

    /**
     * 处理 Embedding 开始事件
     * <p>
     * 在事务提交后异步执行 Embedding 计算。
     * </p>
     *
     * @param event Embedding 开始事件
     */
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmbeddingStart(EmbeddingStartEvent event) {
        log.info("Received EmbeddingStartEvent after transaction commit, processing document: {}", event.getDocumentId());
        documentAsyncService.embedChunksAsync(event.getDocumentId(), event.getEmbeddingConfig());
    }
}