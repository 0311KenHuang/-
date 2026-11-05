package com.envmon.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/** 健康检查：不需要登录，供容器探针与网关使用 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("status", "ok");
        m.put("ts", System.currentTimeMillis());
        return m;
    }
}
