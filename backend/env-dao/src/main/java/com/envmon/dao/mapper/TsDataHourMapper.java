package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.dto.FactorHourStat;
import com.envmon.dao.dto.HourAggRow;
import com.envmon.dao.entity.TsDataHour;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/** 小时数据读写：由分钟数据聚合而来，唯一键 (station_id, factor_code, ts) 幂等覆盖 */
@Mapper
public interface TsDataHourMapper extends BaseMapper<TsDataHour> {

    @Insert("<script>"
            + "INSERT INTO ts_data_hour "
            + "(tenant_id, station_id, factor_code, ts, avg_value, max_value, min_value, "
            + "valid_cnt, total_cnt, valid_rate, is_valid, flag) VALUES "
            + "<foreach collection='list' item='i' separator=','>"
            + "(#{i.tenantId}, #{i.stationId}, #{i.factorCode}, #{i.ts}, #{i.avgValue}, #{i.maxValue}, "
            + "#{i.minValue}, #{i.validCnt}, #{i.totalCnt}, #{i.validRate}, #{i.isValid}, #{i.flag})"
            + "</foreach>"
            + " ON DUPLICATE KEY UPDATE "
            + "avg_value = VALUES(avg_value), max_value = VALUES(max_value), min_value = VALUES(min_value), "
            + "valid_cnt = VALUES(valid_cnt), total_cnt = VALUES(total_cnt), valid_rate = VALUES(valid_rate), "
            + "is_valid = VALUES(is_valid), flag = VALUES(flag), updated_at = NOW()"
            + "</script>")
    int upsertBatch(@Param("list") List<TsDataHour> list);

    /**
     * 日均聚合的原料：按 (站房, 因子) 汇总区间内的小时数据。
     *
     * stopped_hours 是当天落在停运区间内的小时数（区间可能跨天，按交集裁剪），
     * 日均分母用 24 - stopped_hours，落实"停运期间不参与统计"。
     */
    @Select("SELECT h.tenant_id, h.station_id, h.factor_code, COUNT(*) AS total_hour_cnt, "
            + "COALESCE(SUM(h.is_valid), 0) AS valid_hour_cnt, "
            + "AVG(CASE WHEN h.is_valid = 1 THEN h.avg_value END) AS avg_valid, "
            + "MAX(CASE WHEN h.is_valid = 1 THEN h.max_value END) AS max_valid, "
            + "MIN(CASE WHEN h.is_valid = 1 THEN h.min_value END) AS min_valid, "
            + "AVG(h.avg_value) AS avg_all, MAX(h.max_value) AS max_all, MIN(h.min_value) AS min_all, "
            + "COALESCE(("
            + "  SELECT SUM(TIMESTAMPDIFF(SECOND, "
            + "    GREATEST(st.begin_at, #{from}), LEAST(COALESCE(st.end_at, #{to}), #{to})"
            + "  )) / 3600 "
            + "  FROM md_station_stop st "
            + "  WHERE st.station_id = h.station_id "
            + "    AND st.begin_at < #{to} AND (st.end_at IS NULL OR st.end_at > #{from})"
            + "), 0) AS stopped_hours "
            + "FROM ts_data_hour h WHERE h.ts >= #{from} AND h.ts < #{to} "
            + "GROUP BY h.tenant_id, h.station_id, h.factor_code")
    List<HourAggRow> aggregateHours(@Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);

    /** 某站房区间内各因子的有效率明细 */
    @Select("SELECT factor_code, COUNT(*) AS total_hour_cnt, COALESCE(SUM(is_valid), 0) AS valid_hour_cnt, "
            + "AVG(CASE WHEN is_valid = 1 THEN avg_value END) AS avg_value "
            + "FROM ts_data_hour WHERE station_id = #{stationId} AND ts >= #{from} AND ts < #{to} "
            + "GROUP BY factor_code ORDER BY factor_code")
    List<FactorHourStat> statByFactor(@Param("stationId") Long stationId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);
}
