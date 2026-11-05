package com.envmon.api.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    private static final String[] WHITE_LIST = {
            "/health", "/api/health",
            "/auth/login", "/api/auth/login",
            // 设备 HTTP 上报：设备无法携带 JWT，由接入令牌校验
            "/ingest/**", "/api/ingest/**"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle ->
                        SaRouter.match("/**").notMatch(WHITE_LIST).check(r -> StpUtil.checkLogin())))
                .addPathPatterns("/**")
                .order(0);

        // 必须在鉴权之后执行
        registry.addInterceptor(new TenantContextInterceptor())
                .addPathPatterns("/**")
                .order(1);
    }
}
