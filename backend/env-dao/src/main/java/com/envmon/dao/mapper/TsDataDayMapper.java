package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.TsDataDay;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/** 日数据读写：由小时数据聚合而来，唯一键 (station_id, factor_code, ts) 幂等覆盖 */
@Mapper
public interface TsDataDayMapper extends BaseMapper<TsDataDay> {

    @Insert("<script>"
            + "INSERT INTO ts_data_day "
            + "(tenant_id, station_id, factor_code, ts, avg_value, max_value, min_value, emission, "
            + "valid_hour_cnt, total_hour_cnt, expected_hour_cnt, valid_rate, is_valid) VALUES "
            + "<foreach collection='list' item='i' separator=','>"
            + "(#{i.tenantId}, #{i.stationId}, #{i.factorCode}, #{i.ts}, #{i.avgValue}, #{i.maxValue}, "
            + "#{i.minValue}, #{i.emission}, #{i.validHourCnt}, #{i.totalHourCnt}, #{i.expectedHourCnt}, "
            + "#{i.validRate}, #{i.isValid})"
            + "</foreach>"
            + " ON DUPLICATE KEY UPDATE "
            + "avg_value = VALUES(avg_value), max_value = VALUES(max_value), min_value = VALUES(min_value), "
            + "emission = VALUES(emission), valid_hour_cnt = VALUES(valid_hour_cnt), "
            + "total_hour_cnt = VALUES(total_hour_cnt), expected_hour_cnt = VALUES(expected_hour_cnt), "
            + "valid_rate = VALUES(valid_rate), is_valid = VALUES(is_valid), updated_at = NOW()"
            + "</script>")
    int upsertBatch(@Param("list") List<TsDataDay> list);

    @Select("SELECT * FROM ts_data_day WHERE station_id = #{stationId} AND ts >= #{from} AND ts < #{to} "
            + "ORDER BY factor_code")
    List<TsDataDay> selectByStationRange(@Param("stationId") Long stationId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);
}
