package com.envmon.service.notify;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageTemplateServiceTest {

    private final MessageTemplateService service = new MessageTemplateService(null);

    @Test
    void 占位符全部替换() {
        Map<String, String> vars = new LinkedHashMap<String, String>();
        vars.put("level", "P1");
        vars.put("stationName", "丰华纸业-水");

        assertEquals("【P1】丰华纸业-水", MessageTemplateService.renderText("【{{level}}】{{stationName}}", vars));
    }

    @Test
    void 未知占位符不会残留成异常字符() {
        Map<String, String> vars = new LinkedHashMap<String, String>();
        vars.put("value", "42.6");

        assertEquals("值 42.6 {{unknown}}", MessageTemplateService.renderText("值 {{value}} {{unknown}}", vars));
    }

    @Test
    void 变量表包含数值与时间() {
        NotifyMessage message = new NotifyMessage();
        message.setLevel("P1");
        message.setStationName("丰华纸业-水");
        message.setFactorCode("a21026");
        message.setValue(new BigDecimal("42.600"));
        message.setThreshold(new BigDecimal("40"));
        message.setEventNo("AL202611040001");
        message.setBeginAt(LocalDateTime.of(2026, 11, 4, 14, 17));
        message.setStage(1);

        Map<String, String> vars = service.variables(message);

        assertEquals("42.600", vars.get("value"));
        assertEquals("40", vars.get("threshold"));
        assertEquals("11-04 14:17", vars.get("beginAt"));
        assertEquals("1", vars.get("stage"));
        assertTrue(MessageTemplateService.renderText("{{factorCode}} {{value}} / {{threshold}}", vars)
                .contains("a21026 42.600 / 40"));
    }
}
