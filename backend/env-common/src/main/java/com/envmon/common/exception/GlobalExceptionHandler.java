package com.envmon.common.exception;

import com.envmon.common.api.ApiResponse;
import com.envmon.common.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局异常处理：把任何异常收敛成统一响应体 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBiz(BizException e) {
        ApiResponse<Void> body = ApiResponse.fail(e.getCode(), e.getMessage());
        body.setTraceId(TenantContextHolder.traceId());
        return ResponseEntity.status(e.getHttpStatus()).body(body);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception e) {
        String msg = ErrorCode.BAD_REQUEST.getMessage();
        if (e instanceof MethodArgumentNotValidException) {
            FieldError fe = ((MethodArgumentNotValidException) e).getBindingResult().getFieldError();
            if (fe != null) {
                msg = fe.getField() + ": " + fe.getDefaultMessage();
            }
        } else if (e instanceof BindException) {
            FieldError fe = ((BindException) e).getBindingResult().getFieldError();
            if (fe != null) {
                msg = fe.getField() + ": " + fe.getDefaultMessage();
            }
        }
        ApiResponse<Void> body = ApiResponse.fail(ErrorCode.BAD_REQUEST.getCode(), msg);
        body.setTraceId(TenantContextHolder.traceId());
        return ResponseEntity.ok(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception e) {
        String traceId = TenantContextHolder.traceId();
        log.error("[{}] 未处理异常", traceId, e);
        ApiResponse<Void> body = ApiResponse.fail(ErrorCode.INTERNAL.getCode(), ErrorCode.INTERNAL.getMessage());
        body.setTraceId(traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
