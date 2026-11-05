package com.envmon.ts.parse;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Hj212DataParserTest {

    @Test
    void 解析单组分钟数据() {
        String cp = "DataTime=20261104141700;a21026-Rtd=33.215,a21026-Flag=N;"
                + "a00000-Rtd=42.650,a00000-Flag=N";
        List<CpSample> samples = Hj212DataParser.parse(cp, null, null);

        assertEquals(1, samples.size());
        CpSample s = samples.get(0);
        assertEquals(LocalDateTime.of(2026, 11, 4, 14, 17), s.getDataTime());
        assertEquals(2, s.getFactors().size());
        assertEquals(new BigDecimal("33.215"), s.getFactors().get("a21026").getValue());
        assertEquals("N", s.getFactors().get("a21026").getDevFlag());
    }

    @Test
    void 补传报文按DataTime分组() {
        String cp = "DataTime=20261104141500;a21026-Rtd=30.0,a21026-Flag=N;"
                + "DataTime=20261104141600;a21026-Rtd=31.0,a21026-Flag=D";
        List<CpSample> samples = Hj212DataParser.parse(cp, null, null);

        assertEquals(2, samples.size());
        assertEquals(LocalDateTime.of(2026, 11, 4, 14, 15), samples.get(0).getDataTime());
        assertEquals("N", samples.get(0).getFactors().get("a21026").getDevFlag());
        assertEquals(LocalDateTime.of(2026, 11, 4, 14, 16), samples.get(1).getDataTime());
        assertEquals("D", samples.get(1).getFactors().get("a21026").getDevFlag());
    }

    @Test
    void DataTime缺失时退回QN() {
        String cp = "a21026-Rtd=12.5,a21026-Flag=N";
        List<CpSample> samples = Hj212DataParser.parse(cp, "20261104141700001", null);

        assertEquals(1, samples.size());
        assertEquals(LocalDateTime.of(2026, 11, 4, 14, 17), samples.get(0).getDataTime());
    }

    @Test
    void 非法数值置空但保留标记() {
        String cp = "DataTime=20261104141700;a21026-Rtd=--,a21026-Flag=D";
        List<CpSample> samples = Hj212DataParser.parse(cp, null, null);

        assertEquals(1, samples.size());
        assertNull(samples.get(0).getFactors().get("a21026").getValue());
        assertEquals("D", samples.get(0).getFactors().get("a21026").getDevFlag());
    }

    @Test
    void 多标记按优先级取最严重() {
        assertEquals("D", Hj212DataParser.normalizeFlag("N,D"));
        assertEquals("C", Hj212DataParser.normalizeFlag("M,C"));
        assertEquals("T", Hj212DataParser.normalizeFlag("Td,T"));
        assertEquals("N", Hj212DataParser.normalizeFlag("N"));
        assertNull(Hj212DataParser.normalizeFlag(""));
    }

    @Test
    void 折算值与折算标记不当作实测值() {
        String cp = "DataTime=20261104141700;a21002-Rtd=20.0,a21002-ZsRtd=22.5,"
                + "a21002-Flag=N,a21002-ZsFlag=N";
        List<CpSample> samples = Hj212DataParser.parse(cp, null, null);

        assertEquals(new BigDecimal("20.0"), samples.get(0).getFactors().get("a21002").getValue());
    }

    @Test
    void 只解析分钟类命令() {
        assertTrue(Hj212DataParser.isMinuteData("2051"));
        assertTrue(Hj212DataParser.isMinuteData("2011"));
        assertTrue(Hj212DataParser.isMinuteData("2041"));
        assertFalse(Hj212DataParser.isMinuteData("2061"));
        assertFalse(Hj212DataParser.isMinuteData("2031"));
        assertFalse(Hj212DataParser.isMinuteData(null));
    }
}
