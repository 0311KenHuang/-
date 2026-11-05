package com.envmon.api.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import com.envmon.common.api.ApiResponse;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.common.exception.ErrorCode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 异常单独处理，保证 401/403 的 HTTP 语义。
 * 优先级高于通用异常处理器，避免被 50000 吞掉。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SaTokenExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotLogin(NotLoginException e) {
        ApiResponse<Void> body = ApiResponse.fail(ErrorCode.UNAUTHORIZED.getCode(), "未登录或令牌已失效");
        body.setTraceId(TenantContextHolder.traceId());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotRole(NotRoleException e) {
        ApiResponse<Void> body = ApiResponse.fail(ErrorCode.FORBIDDEN.getCode(),
                "需要角色：" + e.getRole());
        body.setTraceId(TenantContextHolder.traceId());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
}
