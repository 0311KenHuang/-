package com.envmon.common.exception;

/**
 * 错误码与前端 errorCode 表一一对应，改动需同步前端。
 */
public enum ErrorCode {

    OK(0, "ok"),
    BAD_REQUEST(40000, "请求参数不合法"),
    UNAUTHORIZED(40100, "未登录或令牌失效"),
    TENANT_MISSING(40101, "缺少租户上下文"),
    FORBIDDEN(40300, "无权访问"),
    DATA_SCOPE_DENIED(40301, "超出数据权限范围"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "数据冲突"),
    INTERNAL(50000, "系统繁忙，请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
