package com.envmon.service.notify.channel;

import com.envmon.service.notify.NotifyChannel;
import com.envmon.service.notify.NotifyMessage;
import com.envmon.service.notify.NotifyProperties;
import com.envmon.service.notify.NotifyResult;
import com.envmon.service.notify.Recipient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 短信兜底：对接通用短信网关（POST {phone, content}）。
 * 网关地址由 notify.sms.gateway-url 配置，模板报备与计费在网关侧处理。
 */
@Component
@RequiredArgsConstructor
public class SmsChannel implements NotifyChannel {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final NotifyProperties props;
    private final HttpGateway http;

    @Override
    public String code() {
        return "SMS";
    }

    @Override
    public boolean configured() {
        return props.getSms().isEnabled() && StringUtils.isNotBlank(props.getSms().getGatewayUrl());
    }

    @Override
    public NotifyResult send(NotifyMessage message, Recipient recipient) {
        if (recipient == null || StringUtils.isBlank(recipient.getPhone())) {
            return NotifyResult.skipped("接收人未登记手机号，短信跳过");
        }
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("phone", recipient.getPhone());
            payload.put("content", message.getTitle() + "：" + message.getBody());
            http.postJson(props.getSms().getGatewayUrl(), MAPPER.writeValueAsString(payload));
            return NotifyResult.ok((int) (System.currentTimeMillis() - start));
        } catch (Exception e) {
            return NotifyResult.fail(e.getMessage(), (int) (System.currentTimeMillis() - start));
        }
    }
}
