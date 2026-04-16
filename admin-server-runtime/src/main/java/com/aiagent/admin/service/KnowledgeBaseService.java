package com.aiagent.admin.service;

import com.aiagent.admin.api.dto.KnowledgeBaseRequest;
import com.aiagent.admin.api.dto.KnowledgeBaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 知识库服务接口
 * <p>
 * 提供知识库管理功能：
 * <ul>
 *   <li>创建、查询、更新、删除知识库</li>
 *   <li>统计知识库文档和分块数量</li>
 *   <li>更新知识库统计数据</li>
 * </ul>
 * </p>
 *
 * @see com.aiagent.admin.domain.entity.KnowledgeBase
 */
public interface KnowledgeBaseService {

    /**
     * 创建知识库
     *
     * @param request   知识库请求
     * @param createdBy 创建人
     * @return 创建的知识库响应
     */
    KnowledgeBaseResponse createKnowledgeBase(KnowledgeBaseRequest request, String createdBy);

    /**
     * 更新知识库
     *
     * @param id      知识库 ID
     * @param request 知识库请求
     * @return 更新后的知识库响应
     */
    KnowledgeBaseResponse updateKnowledgeBase(String id, KnowledgeBaseRequest request);

    /**
     * 获取知识库详情
     *
     * @param id 知识库 ID
     * @return 知识库响应
     */
    KnowledgeBaseResponse getKnowledgeBase(String id);

    /**
     * 分页获取知识库列表
     *
     * @param createdBy 创建人
     * @param pageable  分页参数
     * @return 知识库分页列表
     */
    Page<KnowledgeBaseResponse> listKnowledgeBases(String createdBy, Pageable pageable);

    /**
     * 获取所有知识库列表（不分页）
     *
     * @return 知识库列表
     */
    List<KnowledgeBaseResponse> listAllKnowledgeBases();

    /**
     * 删除知识库
     * <p>
     * 如果知识库下有文档，将抛出异常阻止删除。
     * </p>
     *
     * @param id 知识库 ID
     */
    void deleteKnowledgeBase(String id);

    /**
     * 更新知识库统计数据
     * <p>
     * 重新计算知识库的文档数量和分块数量。
     * </p>
     *
     * @param knowledgeBaseId 知识库 ID
     */
    void updateStatistics(String knowledgeBaseId);
}