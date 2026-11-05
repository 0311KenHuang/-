package com.envmon.api.config;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 安全响应头：API 响应统一附加，防 MIME 嗅探、点击劫持与敏感接口缓存。
 *
 * 注：前端静态资源由 Vite/部署层 nginx 提供，部署时应同步配置
 * Content-Security-Policy（后端只服务 JSON API，此处 CSP 意义有限）。
 */
@Component
@Order(Integer.MIN_VALUE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        // API 响应不缓存：报警/任务列表等实时性数据被中间层缓存会出脏数据
        response.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }
}
