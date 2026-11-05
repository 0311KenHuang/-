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
 * 小程序订阅消息。
 *
 * 一次性订阅要求用户每次授权，长期订阅环保类目大概率不获批（docs/04 §7）。
 * 这里只做网关适配：未配置网关或用户未绑定 openid 时明确记 SKIPPED，
 * 不假装"已送达"；小程序内报警列表才是可靠主入口。
 */
@Component
@RequiredArgsConstructor
public class WxaChannel implements NotifyChannel {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final NotifyProperties props;
    private final HttpGateway http;

    @Override
    public String code() {
        return "WXA";
    }

    @Override
    public boolean configured() {
        return props.getWxa().isEnabled() && StringUtils.isNotBlank(props.getWxa().getGatewayUrl());
    }

    @Override
    public NotifyResult send(NotifyMessage message, Recipient recipient) {
        if (recipient == null || StringUtils.isBlank(recipient.getOpenid())) {
            return NotifyResult.skipped("用户未绑定小程序 openid 或未授权订阅");
        }
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("openid", recipient.getOpenid());
            payload.put("title", message.getTitle());
            payload.put("content", message.getBody());
            payload.put("eventId", message.getEventId());
            http.postJson(props.getWxa().getGatewayUrl(), MAPPER.writeValueAsString(payload));
            return NotifyResult.ok((int) (System.currentTimeMillis() - start));
        } catch (Exception e) {
            return NotifyResult.fail(e.getMessage(), (int) (System.currentTimeMillis() - start));
        }
    }
}
