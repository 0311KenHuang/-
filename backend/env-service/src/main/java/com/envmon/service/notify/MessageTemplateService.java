package com.envmon.service.notify;

import com.envmon.common.context.TenantContextHolder;
import com.envmon.dao.entity.MsgTemplate;
import com.envmon.dao.mapper.MsgTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模板渲染：库里有模板就用库里的，没配就用内置默认值。
 *
 * 占位符 {{key}} 直接替换，不引表达式引擎，避免规则配置变成代码注入面。
 */
@Service
@RequiredArgsConstructor
public class MessageTemplateService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private static final String DEFAULT_TITLE = "【{{level}}】{{ruleName}} · {{stationName}}";
    private static final String DEFAULT_BODY = "因子 {{factorCode}} 值 {{value}}，限值 {{threshold}}，"
            + "发生时间 {{beginAt}}，事件号 {{eventNo}}";

    private final MsgTemplateMapper templateMapper;

    public void render(NotifyMessage message, String channel) {
        MsgTemplate template = templateMapper.selectTemplate(
                TenantContextHolder.tenantId(), message.getEventType(), channel);
        String title = template == null || StringUtils.isBlank(template.getTitleTpl())
                ? DEFAULT_TITLE : template.getTitleTpl();
        String body = template == null || StringUtils.isBlank(template.getBodyTpl())
                ? DEFAULT_BODY : template.getBodyTpl();
        Map<String, String> vars = variables(message);
        message.setTitle(renderText(title, vars));
        message.setBody(renderText(body, vars));
    }

    public Map<String, String> variables(NotifyMessage message) {
        Map<String, String> vars = new LinkedHashMap<String, String>();
        vars.put("level", text(message.getLevel()));
        vars.put("ruleName", text(message.getRuleName()));
        vars.put("stationName", text(message.getStationName()));
        vars.put("factorCode", text(message.getFactorCode()));
        vars.put("value", text(message.getValue() == null ? null : message.getValue().toPlainString()));
        vars.put("threshold", text(message.getThreshold() == null ? null : message.getThreshold().toPlainString()));
        vars.put("beginAt", message.getBeginAt() == null ? "" : TIME.format(message.getBeginAt()));
        vars.put("eventNo", text(message.getEventNo()));
        vars.put("eventType", text(message.getEventType()));
        vars.put("stage", String.valueOf(message.getStage() == null ? 0 : message.getStage()));
        return vars;
    }

    public static String renderText(String tpl, Map<String, String> vars) {
        if (tpl == null) {
            return "";
        }
        String out = tpl;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            out = out.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    private String text(String s) {
        return s == null ? "" : s;
    }
}
