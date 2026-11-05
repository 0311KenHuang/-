package com.envmon.common.web;

import com.envmon.common.api.ApiResponse;
import com.envmon.common.context.TenantContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一包装：controller 只返回业务对象，这里包成 {code,message,data,traceId}。
 * 已在 controller 内手工构造 ApiResponse 的，不重复包装。
 */
@RestControllerAdvice
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        Class<?> type = returnType.getParameterType();
        if (ApiResponse.class.isAssignableFrom(type)) {
            return false;
        }
        // String / byte[] 交给原始转换器，否则会破坏文件下载与纯文本
        return !CharSequence.class.isAssignableFrom(type)
                && !byte[].class.isAssignableFrom(type)
                && !type.isArray();
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof ApiResponse) {
            return body;
        }
        ApiResponse<Object> wrapped = ApiResponse.ok(body);
        wrapped.setTraceId(TenantContextHolder.traceId());
        return wrapped;
    }
}
