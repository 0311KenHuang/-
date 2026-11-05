package com.envmon.ts.calc;

import com.envmon.ts.service.MinuteDataService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ValueRulesTest {

    private final Map<String, TsProperties.NegativeFix> fixes = new HashMap<String, TsProperties.NegativeFix>();

    ValueRulesTest() {
        TsProperties.NegativeFix so2 = new TsProperties.NegativeFix();
        so2.setLowerBound(new BigDecimal("-15"));
        so2.setTarget(new BigDecimal("0.5"));
        fixes.put("a21002", so2);
    }

    @Test
    void 超量程按上限夹取并补T标记() {
        ValueRules.Result r = ValueRules.apply("a21026", new BigDecimal("1200"), "N",
                new BigDecimal("1000"), fixes);

        assertEquals(0, new BigDecimal("1000").compareTo(r.getValue()));
        assertEquals("T", r.getDevFlag());
    }

    @Test
    void 量程内不改值也不改标记() {
        ValueRules.Result r = ValueRules.apply("a21026", new BigDecimal("33.2"), "N",
                new BigDecimal("1000"), fixes);

        assertEquals(0, new BigDecimal("33.2").compareTo(r.getValue()));
        assertEquals("N", r.getDevFlag());
    }

    @Test
    void 无量程配置时不夹取() {
        ValueRules.Result r = ValueRules.apply("a21026", new BigDecimal("99999"), "N", null, fixes);

        assertEquals(0, new BigDecimal("99999").compareTo(r.getValue()));
        assertEquals("N", r.getDevFlag());
    }

    @Test
    void 设备已标故障时保留原标记() {
        ValueRules.Result r = ValueRules.apply("a21026", new BigDecimal("1200"), "D",
                new BigDecimal("1000"), fixes);

        assertEquals(0, new BigDecimal("1000").compareTo(r.getValue()));
        assertEquals("D", r.getDevFlag());
    }

    @Test
    void 负值修约只对配置因子生效() {
        ValueRules.Result so2 = ValueRules.apply("a21002", new BigDecimal("-3.2"), "N", null, fixes);
        assertEquals(0, new BigDecimal("0.5").compareTo(so2.getValue()));

        ValueRules.Result other = ValueRules.apply("a21026", new BigDecimal("-3.2"), "N", null, fixes);
        assertEquals(0, new BigDecimal("-3.2").compareTo(other.getValue()));
    }

    @Test
    void 超过修约下限的负值不动() {
        ValueRules.Result r = ValueRules.apply("a21002", new BigDecimal("-20"), "N", null, fixes);

        assertEquals(0, new BigDecimal("-20").compareTo(r.getValue()));
    }

    @Test
    void 空值不参与修约() {
        ValueRules.Result r = ValueRules.apply("a21002", null, "D", new BigDecimal("100"), fixes);

        assertNull(r.getValue());
        assertEquals("D", r.getDevFlag());
    }

    @Test
    void 分钟有效性按标记判定() {
        assertEquals(1, MinuteDataService.qualityOf(new BigDecimal("1"), "N"));
        assertEquals(1, MinuteDataService.qualityOf(new BigDecimal("1"), "T"));
        assertEquals(2, MinuteDataService.qualityOf(new BigDecimal("1"), "D"));
        assertEquals(2, MinuteDataService.qualityOf(new BigDecimal("1"), "Td"));
        assertEquals(2, MinuteDataService.qualityOf(null, "N"));
        assertEquals(3, MinuteDataService.qualityOf(new BigDecimal("1"), null));
    }
}
