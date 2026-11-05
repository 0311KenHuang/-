package com.envmon.ts.calc;

import com.envmon.dao.dto.HourAggRow;
import com.envmon.dao.dto.MinuteAggRow;
import com.envmon.dao.entity.TsDataDay;
import com.envmon.dao.entity.TsDataHour;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AggregationCalculatorTest {

    private final TsProperties props = new TsProperties();
    private final LocalDateTime hour = LocalDateTime.of(2026, 11, 4, 14, 0);

    @Test
    void 四十五个有效分钟判定有效() {
        TsDataHour h = AggregationCalculator.hour(minuteRow(45, 60, "10.0"), hour, props);

        assertEquals(1, h.getIsValid());
        assertEquals("N", h.getFlag());
        assertEquals(0, new BigDecimal("0.7500").compareTo(h.getValidRate()));
        assertEquals(0, new BigDecimal("10.0").compareTo(h.getAvgValue()));
    }

    @Test
    void 四十四个有效分钟判定无效() {
        TsDataHour h = AggregationCalculator.hour(minuteRow(44, 60, "10.0"), hour, props);

        assertEquals(0, h.getIsValid());
        assertEquals("U", h.getFlag());
    }

    @Test
    void 无效小时取最严重标记() {
        MinuteAggRow row = minuteRow(44, 60, "10.0");
        row.setDCnt(3);
        TsDataHour h = AggregationCalculator.hour(row, hour, props);

        assertEquals(0, h.getIsValid());
        assertEquals("D", h.getFlag());
    }

    @Test
    void 无有效数据时降级用全部数据计算() {
        MinuteAggRow row = new MinuteAggRow();
        row.setTenantId(1L);
        row.setStationId(1L);
        row.setFactorCode("a21026");
        row.setTotalCnt(10);
        row.setValidCnt(0);
        row.setAvgAll(new BigDecimal("8.5"));
        row.setMaxAll(new BigDecimal("9.0"));
        row.setMinAll(new BigDecimal("8.0"));
        row.setDCnt(10);

        TsDataHour h = AggregationCalculator.hour(row, hour, props);

        assertEquals(0, h.getIsValid());
        assertEquals(0, new BigDecimal("8.5").compareTo(h.getAvgValue()));
        assertEquals("D", h.getFlag());
    }

    @Test
    void 十八个有效小时判定日均有效() {
        TsDataDay d = AggregationCalculator.day(hourRow(18, 24, "20.0"),
                LocalDate.of(2026, 11, 4), props);

        assertEquals(1, d.getIsValid());
        assertEquals(0, new BigDecimal("0.7500").compareTo(d.getValidRate()));
        assertEquals(0, new BigDecimal("20.0").compareTo(d.getAvgValue()));
    }

    @Test
    void 十七个有效小时判定日均无效() {
        TsDataDay d = AggregationCalculator.day(hourRow(17, 24, "20.0"),
                LocalDate.of(2026, 11, 4), props);

        assertEquals(0, d.getIsValid());
    }

    @Test
    void 停运小时从日均分母扣除() {
        HourAggRow row = hourRow(12, 12, "20.0");
        row.setStoppedHours(new BigDecimal("12"));
        TsDataDay d = AggregationCalculator.day(row, LocalDate.of(2026, 11, 4), props);

        assertEquals(12, d.getExpectedHourCnt());
        assertEquals(1, d.getIsValid());
        assertEquals(0, new BigDecimal("1.0000").compareTo(d.getValidRate()));
    }

    @Test
    void 扣停运后仍不足75判无效() {
        HourAggRow row = hourRow(8, 12, "20.0");
        row.setStoppedHours(new BigDecimal("12"));
        TsDataDay d = AggregationCalculator.day(row, LocalDate.of(2026, 11, 4), props);

        assertEquals(12, d.getExpectedHourCnt());
        assertEquals(0, d.getIsValid());
    }

    private MinuteAggRow minuteRow(int valid, int total, String value) {
        MinuteAggRow row = new MinuteAggRow();
        row.setTenantId(1L);
        row.setStationId(1L);
        row.setFactorCode("a21026");
        row.setValidCnt(valid);
        row.setTotalCnt(total);
        row.setAvgValid(new BigDecimal(value));
        row.setMaxValid(new BigDecimal(value));
        row.setMinValid(new BigDecimal(value));
        row.setAvgAll(new BigDecimal(value));
        row.setMaxAll(new BigDecimal(value));
        row.setMinAll(new BigDecimal(value));
        row.setDCnt(0);
        row.setCCnt(0);
        row.setMCnt(0);
        row.setTCnt(0);
        row.setTdCnt(0);
        row.setNoFlagCnt(0);
        return row;
    }

    private HourAggRow hourRow(int valid, int total, String value) {
        HourAggRow row = new HourAggRow();
        row.setTenantId(1L);
        row.setStationId(1L);
        row.setFactorCode("a21026");
        row.setValidHourCnt(valid);
        row.setTotalHourCnt(total);
        row.setAvgValid(new BigDecimal(value));
        row.setMaxValid(new BigDecimal(value));
        row.setMinValid(new BigDecimal(value));
        row.setAvgAll(new BigDecimal(value));
        row.setMaxAll(new BigDecimal(value));
        row.setMinAll(new BigDecimal(value));
        return row;
    }
}
