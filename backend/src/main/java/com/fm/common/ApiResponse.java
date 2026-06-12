package com.fm.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应结构
 */
@Data
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String traceId;
    private String message;
    private T data;

    private ApiResponse() {}

    public static <T> ApiResponse<T> success(String traceId, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.traceId = traceId;
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> success(String traceId) {
        return success(traceId, null);
    }

    public static <T> ApiResponse<T> fail(String traceId, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.traceId = traceId;
        response.message = message;
        return response;
    }
}
