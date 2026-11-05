package com.envmon.common.api;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体。业务码走 code，HTTP 状态码只承载传输语义。
 * 与前端约定的结构：{ code, message, data, traceId }
 */
@Data
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;
    private String traceId;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<T>();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        ApiResponse<T> r = new ApiResponse<T>();
        r.code = code;
        r.message = message;
        return r;
    }
}
