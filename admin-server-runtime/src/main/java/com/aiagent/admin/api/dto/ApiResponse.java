package com.aiagent.admin.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应包装类
 * <p>
 * 所有 REST API 接口的返回值都使用此类包装，提供统一的响应格式。
 * 包含响应码、消息、数据和成功标识。
 * </p>
 *
 * @param <T> 响应数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 响应状态码（200 表示成功，其他表示失败）
     */
    private Integer code;

    /** 响应消息（成功时为 "Success"，失败时为错误描述） */
    private String message;

    /** 响应数据 */
    private T data;

    /** 是否成功标识 */
    private Boolean success;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("Success")
                .data(data)
                .success(true)
                .build();
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .code(500)
                .message(message)
                .success(false)
                .build();
    }

    public static <T> ApiResponse<T> error(Integer code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .success(false)
                .build();
    }
}
