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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 飞书群机器人交互卡片。
 *
 * 本期走 Webhook：5 分钟接入、无需审批，但只能发消息、不能收按钮回调，
 * 所以卡片上只放"查看详情"跳转；"认领"按钮需要企业自建应用 + 公网回调地址，
 * 拿到权限后再补，文档 docs/04 §8 有明确限制说明。
 */
@Component
@RequiredArgsConstructor
public class FeishuChannel implements NotifyChannel {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final NotifyProperties props;
    private final HttpGateway http;

    @Override
    public String code() {
        return "FEISHU";
    }

    @Override
    public boolean configured() {
        return props.getFeishu().isEnabled()
                && StringUtils.isNotBlank(props.getFeishu().getWebhookUrl());
    }

    @Override
    public NotifyResult send(NotifyMessage message, Recipient recipient) {
        long start = System.currentTimeMillis();
        try {
            String json = MAPPER.writeValueAsString(card(message, recipient));
            String body = http.postJson(props.getFeishu().getWebhookUrl(), json);
            int cost = (int) (System.currentTimeMillis() - start);
            // 机器人返回 {"StatusCode":0,...}，应用消息返回 {"code":0,...}；
            // 先去掉空白，兼容 ": 0" 这类格式化输出
            String compact = body == null ? "" : body.replaceAll("\\s", "");
            if (!(compact.contains("\"StatusCode\":0") || compact.contains("\"code\":0"))) {
                return NotifyResult.fail("飞书返回非成功: " + abbreviate(body), cost);
            }
            return NotifyResult.ok(cost);
        } catch (Exception e) {
            return NotifyResult.fail(e.getMessage(), (int) (System.currentTimeMillis() - start));
        }
    }

    private Map<String, Object> card(NotifyMessage message, Recipient recipient) {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("msg_type", "interactive");

        Map<String, Object> card = new LinkedHashMap<String, Object>();
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("wide_screen_mode", true);
        card.put("config", config);

        Map<String, Object> header = new LinkedHashMap<String, Object>();
        header.put("template", "P1".equals(message.getLevel()) ? "red"
                : "P2".equals(message.getLevel()) ? "orange" : "blue");
        header.put("title", plainText(message.getTitle()));
        card.put("header", header);

        List<Map<String, Object>> elements = new ArrayList<Map<String, Object>>();
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        fields.put("tag", "div");
        List<Map<String, Object>> fieldList = new ArrayList<Map<String, Object>>();
        fieldList.add(field("**报警类型**", message.getRuleName()));
        fieldList.add(field("**站房 / 因子**", message.getStationName() + " / " + nullToEmpty(message.getFactorCode())));
        fieldList.add(field("**监测值 / 限值**", num(message.getValue()) + " / " + num(message.getThreshold())));
        fieldList.add(field("**发生时间**", message.getBeginAt() == null ? "-" : message.getBeginAt().toString()));
        fields.put("fields", fieldList);
        elements.add(fields);

        Map<String, Object> note = new LinkedHashMap<String, Object>();
        note.put("tag", "div");
        note.put("text", larkMd("事件号 `" + nullToEmpty(message.getEventNo()) + "`　接收人 "
                + (recipient == null ? "-" : recipient.display())
                + (message.getStage() != null && message.getStage() > 0 ? "　【已升级 T+" + message.getStage() + "】" : "")));
        elements.add(note);

        if (StringUtils.isNotBlank(props.getDetailUrl())) {
            Map<String, Object> action = new LinkedHashMap<String, Object>();
            action.put("tag", "action");
            List<Map<String, Object>> buttons = new ArrayList<Map<String, Object>>();
            Map<String, Object> button = new LinkedHashMap<String, Object>();
            button.put("tag", "button");
            button.put("text", plainText("查看详情"));
            button.put("type", "primary");
            button.put("url", props.getDetailUrl() + "?eventId=" + message.getEventId());
            buttons.add(button);
            action.put("actions", buttons);
            elements.add(action);
        }

        card.put("elements", elements);
        root.put("card", card);
        return root;
    }

    private Map<String, Object> field(String label, String content) {
        Map<String, Object> f = new LinkedHashMap<String, Object>();
        f.put("is_short", true);
        f.put("text", larkMd(label + "\n" + content));
        return f;
    }

    private Map<String, Object> plainText(String text) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("tag", "plain_text");
        m.put("content", nullToEmpty(text));
        return m;
    }

    private Map<String, Object> larkMd(String text) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("tag", "lark_md");
        m.put("content", nullToEmpty(text));
        return m;
    }

    private String num(java.math.BigDecimal v) {
        return v == null ? "-" : v.toPlainString();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String abbreviate(String s) {
        return s == null || s.length() <= 200 ? String.valueOf(s) : s.substring(0, 200);
    }
}
