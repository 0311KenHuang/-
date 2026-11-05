package com.envmon.common.exception;

/**
 * 业务异常。默认 HTTP 200 + 业务码，只有认证失败才用 401。
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final int httpStatus;

    public BizException(int code, String message) {
        this(code, message, 200);
    }

    public BizException(int code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static BizException of(ErrorCode ec) {
        return new BizException(ec.getCode(), ec.getMessage());
    }

    public static BizException of(ErrorCode ec, String message) {
        return new BizException(ec.getCode(), message);
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
