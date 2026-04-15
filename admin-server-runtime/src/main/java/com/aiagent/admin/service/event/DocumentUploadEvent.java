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
 */
public class DocumentUploadEvent {

    private final String documentId;
    private final byte[] fileContent;
    private final String contentType;
    private final String originalFilename;
    private final String embeddingModelId;

    public DocumentUploadEvent(String documentId, byte[] fileContent, String contentType,
                               String originalFilename, String embeddingModelId) {
        this.documentId = documentId;
        this.fileContent = fileContent;
        this.contentType = contentType;
        this.originalFilename = originalFilename;
        this.embeddingModelId = embeddingModelId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public String getContentType() {
        return contentType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getEmbeddingModelId() {
        return embeddingModelId;
    }
}