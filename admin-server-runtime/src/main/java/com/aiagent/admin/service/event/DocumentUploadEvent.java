package com.aiagent.admin.service.event;

/**
 * 文档上传完成事件
 * <p>
 * 当文档上传并保存到数据库后触发此事件，
 * 用于异步处理文档（提取文本、分块）。
 * </p>
 * <p>
 * 注意：由于异步线程执行时 HTTP 请求可能已结束，
 * Tomcat 会清理临时上传文件，因此事件中存储的是
 * 文件内容的字节数组，而不是 MultipartFile 对象。
 * </p>
 *
 * @param documentId       文档 ID
 * @param fileContent      文件内容字节数组
 * @param contentType      文件 MIME 类型
 * @param originalFilename 原始文件名
 * @param embeddingModelId Embedding 模型 ID
 */
public record DocumentUploadEvent(String documentId, byte[] fileContent, String contentType, String originalFilename,
                                  String embeddingModelId) {

    /**
     * 构造文档上传事件
     *
     * @param documentId       文档 ID
     * @param fileContent      文件内容字节数组
     * @param contentType      文件 MIME 类型
     * @param originalFilename 原始文件名
     * @param embeddingModelId Embedding 模型 ID
     */
    public DocumentUploadEvent {
    }
}