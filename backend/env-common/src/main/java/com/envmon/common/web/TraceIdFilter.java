package com.envmon.common.web;

import com.envmon.common.context.TenantContext;
import com.envmon.common.context.TenantContextHolder;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * 为每个请求生成 traceId，写入 MDC 与租户上下文。
 * 必须最先执行，后续的认证过滤器在已有上下文上补齐用户信息。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId = request.getHeader(HEADER);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        response.setHeader(HEADER, traceId);

        TenantContext ctx = TenantContextHolder.get();
        if (ctx == null) {
            ctx = new TenantContext();
            TenantContextHolder.set(ctx);
        }
        ctx.setTraceId(traceId);
        MDC.put(MDC_KEY, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
            TenantContextHolder.clear();
        }
    }
}
