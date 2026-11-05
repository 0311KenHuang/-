package com.envmon.service.notify.channel;

import com.envmon.service.notify.NotifyProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** 外部通道共用的 HTTP 客户端：短超时，避免一条慢通道卡住整轮报警扫描 */
@Component
public class HttpGateway {

    private final RestTemplate rest;

    public HttpGateway(NotifyProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeoutMs());
        factory.setReadTimeout(props.getReadTimeoutMs());
        this.rest = new RestTemplate(factory);
    }

    public String postJson(String url, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = rest.postForEntity(url, new HttpEntity<String>(json, headers), String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("HTTP " + response.getStatusCodeValue());
        }
        return response.getBody();
    }
}
