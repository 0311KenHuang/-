package com.envmon.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 最小集成冒烟测试：起真实 Spring 上下文，连本机 MySQL（Flyway 自动迁移）。
 *
 * 运行方式（本机需先启动 MySQL，库 env_mon 已由开发环境迁移）：
 *   RUN_IT=true DB_PASSWORD= ./mvn.sh -pl env-api test
 *
 * 默认不启用：CI/无数据库环境跑 mvn test 不受影响。
 * 覆盖：健康检查、登录成功、密码错误 40100、未登录 401 语义。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.datasource.password:${DB_PASSWORD:}"})
@EnabledIfEnvironmentVariable(named = "RUN_IT", matches = "true")
class ApiSmokeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    private String url(String path) {
        return "http://127.0.0.1:" + port + "/api" + path;
    }

    @Test
    void healthShouldBeOk() {
        ResponseEntity<Map> resp = rest.getForEntity(url("/health"), Map.class);
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals(0, ((Number) resp.getBody().get("code")).intValue());
    }

    @Test
    void loginWithWrongPasswordShouldReturn40100() {
        Map<String, String> body = new HashMap<String, String>();
        body.put("username", "admin");
        body.put("password", "definitely-wrong");
        ResponseEntity<Map> resp = postJson("/auth/login", body);
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals(40100, ((Number) resp.getBody().get("code")).intValue());
    }

    @Test
    void loginWithCorrectPasswordShouldReturnToken() {
        Map<String, String> body = new HashMap<String, String>();
        body.put("username", "admin");
        body.put("password", "Admin@123");
        ResponseEntity<Map> resp = postJson("/auth/login", body);
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals(0, ((Number) resp.getBody().get("code")).intValue());
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertNotNull(data);
        assertTrue(data.get("token").toString().length() > 10, "应返回会话 token");
        Map<String, Object> user = (Map<String, Object>) data.get("user");
        assertEquals("admin", user.get("username"));
    }

    @Test
    void businessApiWithoutTokenShouldReturn401() {
        ResponseEntity<Map> resp = rest.getForEntity(url("/station?page=1&pageSize=5"), Map.class);
        assertEquals(401, resp.getStatusCodeValue());
    }

    private ResponseEntity<Map> postJson(String path, Map<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(url(path), new HttpEntity<>(body, headers), Map.class);
    }
}
