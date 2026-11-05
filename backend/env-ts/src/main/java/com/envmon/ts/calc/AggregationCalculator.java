package com.envmon.ts.calc;

import com.envmon.dao.dto.HourAggRow;
import com.envmon.dao.dto.MinuteAggRow;
import com.envmon.dao.entity.TsDataDay;
import com.envmon.dao.entity.TsDataHour;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 附录 G 纯计算：不碰数据库，便于单测覆盖边界（45/44 分钟、18/17 小时）。
 *
 * 两条降级规则：
 *   1. 小时内没有任何有效分钟时，用全部数据算均值，但小时值仍判无效；
 *   2. 同日无任何有效小时时，用全部小时均值算日均，日数据仍判无效。
 * 对应文档"统计时段内无有效数据时，全部无效数据参与计算"。
 */
public final class AggregationCalculator {

    /** 无有效数据且无任何异常标记时的计算标记：缺数/不足 */
    public static final String FLAG_MISSING = "U";

    private AggregationCalculator() {
    }

    public static TsDataHour hour(MinuteAggRow row, LocalDateTime hourStart, TsProperties props) {
        int total = nz(row.getTotalCnt());
        int valid = nz(row.getValidCnt());
        boolean hasValid = valid > 0;
        boolean hourValid = valid >= props.getHourMinValidMinutes();

        TsDataHour h = new TsDataHour();
        h.setTenantId(row.getTenantId());
        h.setStationId(row.getStationId());
        h.setFactorCode(row.getFactorCode());
        h.setTs(hourStart);
        h.setAvgValue(hasValid ? row.getAvgValid() : row.getAvgAll());
        h.setMaxValue(hasValid ? row.getMaxValid() : row.getMaxAll());
        h.setMinValue(hasValid ? row.getMinValid() : row.getMinAll());
        h.setValidCnt(valid);
        h.setTotalCnt(total);
        h.setValidRate(rate(valid, props.getHourMinutes()));
        h.setIsValid(hourValid ? 1 : 0);
        h.setFlag(hourValid ? "N" : dominantFlag(row));
        return h;
    }

    public static TsDataDay day(HourAggRow row, LocalDate date, TsProperties props) {
        int total = nz(row.getTotalHourCnt());
        int valid = nz(row.getValidHourCnt());
        boolean hasValid = valid > 0;

        // 停运时段不参与统计：分母扣掉停运小时，避免"停了一天也算数据缺失"
        int stopped = row.getStoppedHours() == null
                ? 0 : (int) Math.round(row.getStoppedHours().doubleValue());
        stopped = Math.max(0, Math.min(stopped, props.getDayHours()));
        int expected = Math.max(props.getDayHours() - stopped, 1);
        int required = (int) Math.ceil(expected * props.getDayValidRate().doubleValue());
        boolean dayValid = valid >= required;

        TsDataDay d = new TsDataDay();
        d.setTenantId(row.getTenantId());
        d.setStationId(row.getStationId());
        d.setFactorCode(row.getFactorCode());
        d.setTs(date.atStartOfDay());
        d.setAvgValue(hasValid ? row.getAvgValid() : row.getAvgAll());
        d.setMaxValue(hasValid ? row.getMaxValid() : row.getMaxAll());
        d.setMinValue(hasValid ? row.getMinValid() : row.getMinAll());
        d.setValidHourCnt(valid);
        d.setTotalHourCnt(total);
        d.setExpectedHourCnt(expected);
        d.setValidRate(rate(valid, expected));
        d.setIsValid(dayValid ? 1 : 0);
        return d;
    }

    /** 无效小时取最严重的设备标记；全是 N 但分钟数不够则记 U（缺数） */
    static String dominantFlag(MinuteAggRow row) {
        if (nz(row.getDCnt()) > 0) {
            return "D";
        }
        if (nz(row.getCCnt()) > 0) {
            return "C";
        }
        if (nz(row.getMCnt()) > 0) {
            return "M";
        }
        if (nz(row.getTCnt()) > 0) {
            return "T";
        }
        if (nz(row.getTdCnt()) > 0) {
            return "Td";
        }
        return FLAG_MISSING;
    }

    private static BigDecimal rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal r = BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
        return r.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP) : r;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
