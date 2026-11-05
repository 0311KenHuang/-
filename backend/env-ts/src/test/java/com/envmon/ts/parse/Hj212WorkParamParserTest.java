package com.envmon.ts.parse;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Hj212WorkParamParserTest {

    @Test
    void 参数名归一化并保留因子前缀() {
        assertEquals("SN", Hj212WorkParamParser.canonical("SN"));
        assertEquals("RANGE_MAX", Hj212WorkParamParser.canonical("RangeMax"));
        assertEquals("RANGE_MIN", Hj212WorkParamParser.canonical("RANGE_MIN"));
        assertEquals("DIGEST_TEMP", Hj212WorkParamParser.canonical("Digest_Temp"));
        assertEquals("a21026:RANGE_MAX", Hj212WorkParamParser.canonical("a21026-RangeMax"));
        assertNull(Hj212WorkParamParser.canonical("a21026-Rtd"));
    }

    @Test
    void 只解析白名单参数且忽略浓度值() {
        String cp = "DataTime=20261104141700;SN=SN-FHZY-S-01-0001;"
                + "a21026-RangeMax=500,a21026-Slope=1.02,a21026-Intercept=0.3;"
                + "CAL_CONC=40,DIGEST_TEMP=165;a21026-Rtd=33.215";
        List<WorkParamPoint> points = Hj212WorkParamParser.parse(cp, null, null);

        assertEquals(6, points.size());
        LocalDateTime expected = LocalDateTime.of(2026, 11, 4, 14, 17);
        for (WorkParamPoint p : points) {
            assertEquals(expected, p.getCollectedAt());
            assertFalse("a21026:Rtd".equals(p.getParamCode()));
        }
        assertEquals("SN-FHZY-S-01-0001", points.get(0).getParamValue());
        assertEquals("a21026:RANGE_MAX", points.get(1).getParamCode());
    }

    @Test
    void DataTime缺失时退回QN() {
        List<WorkParamPoint> points = Hj212WorkParamParser.parse(
                "SN=SN001;RANGE_MAX=1000", "20261104141700001", null);

        assertEquals(2, points.size());
        assertEquals(LocalDateTime.of(2026, 11, 4, 14, 17), points.get(0).getCollectedAt());
    }

    @Test
    void 空CP或无参数键返回空列表() {
        assertTrue(Hj212WorkParamParser.parse(null, null, null).isEmpty());
        assertTrue(Hj212WorkParamParser.parse("DataTime=20261104141700;a21026-Rtd=1.0", null, null).isEmpty());
    }

    @Test
    void 接收时间兜底() {
        LocalDateTime fallback = LocalDateTime.of(2026, 11, 4, 14, 20, 33);
        List<WorkParamPoint> points = Hj212WorkParamParser.parse("SN=SN001", null, fallback);

        assertEquals(1, points.size());
        assertEquals(LocalDateTime.of(2026, 11, 4, 14, 20), points.get(0).getCollectedAt());
    }
}
