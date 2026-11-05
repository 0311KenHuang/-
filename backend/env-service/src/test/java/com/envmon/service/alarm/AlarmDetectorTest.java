package com.envmon.service.alarm;

import com.envmon.dao.dto.AlarmSampleRow;
import com.envmon.dao.dto.LastPointRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlarmDetectorTest {

    private final LocalDateTime base = LocalDateTime.of(2026, 11, 4, 14, 0);

    @Test
    void 连续三分钟超限才算命中() {
        List<AlarmSampleRow> rows = Arrays.asList(
                row(base, "50.0"),
                row(base.plusMinutes(1), "52.0"),
                row(base.plusMinutes(2), "55.0"));

        List<AlarmHit> hits = AlarmDetector.overLimit(rows, 3);

        assertEquals(1, hits.size());
        assertEquals(base, hits.get(0).getBeginAt());
        assertEquals(0, new BigDecimal("55.0").compareTo(hits.get(0).getValue()));
    }

    @Test
    void 中间断档不连续不命中() {
        List<AlarmSampleRow> rows = Arrays.asList(
                row(base, "50.0"),
                row(base.plusMinutes(1), "52.0"),
                // 14:02 缺数或已回落，连续计数必须清零
                row(base.plusMinutes(3), "55.0"),
                row(base.plusMinutes(4), "56.0"));

        assertTrue(AlarmDetector.overLimit(rows, 3).isEmpty());
    }

    @Test
    void 十次完全相等判定恒值() {
        List<AlarmSampleRow> rows = new ArrayList<AlarmSampleRow>();
        for (int i = 0; i < 10; i++) {
            rows.add(row(base.plusMinutes(i), "12.500"));
        }

        List<AlarmHit> hits = AlarmDetector.deadValue(rows, 10);

        assertEquals(1, hits.size());
        assertEquals(base, hits.get(0).getBeginAt());
    }

    @Test
    void 只有九条不判定恒值() {
        List<AlarmSampleRow> rows = new ArrayList<AlarmSampleRow>();
        for (int i = 0; i < 9; i++) {
            rows.add(row(base.plusMinutes(i), "12.500"));
        }

        assertTrue(AlarmDetector.deadValue(rows, 10).isEmpty());
    }

    @Test
    void 有一条不同就不算恒值() {
        List<AlarmSampleRow> rows = new ArrayList<AlarmSampleRow>();
        for (int i = 0; i < 10; i++) {
            rows.add(row(base.plusMinutes(i), i == 5 ? "12.501" : "12.500"));
        }

        assertTrue(AlarmDetector.deadValue(rows, 10).isEmpty());
    }

    @Test
    void 超过三十分钟没数据判定缺数() {
        LocalDateTime now = base.plusHours(1);
        LastPointRow last = last(base, "a21026");

        AlarmDetector.MissingResult result = AlarmDetector.missing(
                Collections.singletonList(last), now, 30, Collections.<Long>emptySet());

        assertEquals(1, result.getHits().size());
        assertEquals(base.plusMinutes(1), result.getHits().get(0).getBeginAt());
        assertTrue(result.getFreshKeys().isEmpty());
    }

    @Test
    void 数据仍新鲜时不报缺数() {
        LocalDateTime now = base.plusMinutes(20);
        LastPointRow last = last(base, "a21026");

        AlarmDetector.MissingResult result = AlarmDetector.missing(
                Collections.singletonList(last), now, 30, Collections.<Long>emptySet());

        assertTrue(result.getHits().isEmpty());
        assertTrue(result.getFreshKeys().contains(AlarmDetector.key(1L, "a21026")));
    }

    @Test
    void 停运站房不报缺数并允许关闭遗留事件() {
        LocalDateTime now = base.plusHours(2);
        LastPointRow last = last(base, "a21026");

        AlarmDetector.MissingResult result = AlarmDetector.missing(
                Collections.singletonList(last), now, 30, new HashSet<Long>(Collections.singletonList(1L)));

        assertTrue(result.getHits().isEmpty());
        assertTrue(result.getFreshKeys().contains(AlarmDetector.key(1L, "a21026")));
    }

    private AlarmSampleRow row(LocalDateTime ts, String value) {
        AlarmSampleRow r = new AlarmSampleRow();
        r.setStationId(1L);
        r.setFactorCode("a21026");
        r.setTs(ts);
        r.setValue(new BigDecimal(value));
        r.setThreshold(new BigDecimal("40"));
        return r;
    }

    private LastPointRow last(LocalDateTime ts, String factor) {
        LastPointRow r = new LastPointRow();
        r.setStationId(1L);
        r.setFactorCode(factor);
        r.setLastTs(ts);
        return r;
    }
}
