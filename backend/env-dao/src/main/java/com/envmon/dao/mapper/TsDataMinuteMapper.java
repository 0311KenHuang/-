package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.dto.AlarmSampleRow;
import com.envmon.dao.dto.CountStat;
import com.envmon.dao.dto.FlaggedRow;
import com.envmon.dao.dto.LastPointRow;
import com.envmon.dao.dto.MinuteAggRow;
import com.envmon.dao.entity.TsDataMinute;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分钟数据读写。
 *
 * 写入统一走 upsertBatch：设备重发、断网补传都会重复上报，
 * 唯一键 (station_id, factor_code, ts) + ON DUPLICATE KEY UPDATE 保证幂等，
 * 同一分钟保留最后一次上报（对应"重复报文只更新最后一次"）。
 */
@Mapper
public interface TsDataMinuteMapper extends BaseMapper<TsDataMinute> {

    @Insert("<script>"
            + "INSERT INTO ts_data_minute "
            + "(tenant_id, station_id, factor_code, ts, `value`, raw_value, dev_flag, prod_flag, quality, source_cn, frame_id) VALUES "
            + "<foreach collection='list' item='i' separator=','>"
            + "(#{i.tenantId}, #{i.stationId}, #{i.factorCode}, #{i.ts}, #{i.value}, #{i.rawValue}, "
            + "#{i.devFlag}, #{i.prodFlag}, #{i.quality}, #{i.sourceCn}, #{i.frameId})"
            + "</foreach>"
            + " ON DUPLICATE KEY UPDATE "
            + "`value` = VALUES(`value`), raw_value = VALUES(raw_value), dev_flag = VALUES(dev_flag), "
            + "prod_flag = VALUES(prod_flag), quality = VALUES(quality), source_cn = VALUES(source_cn), "
            + "frame_id = VALUES(frame_id), updated_at = NOW()"
            + "</script>")
    int upsertBatch(@Param("list") List<TsDataMinute> list);

    /** 单站房每个因子取最新一条，用于"站房实时值" */
    @Select("SELECT m.station_id, m.factor_code, m.ts, m.`value`, m.dev_flag, m.quality, m.source_cn "
            + "FROM ts_data_minute m JOIN ("
            + "  SELECT factor_code, MAX(ts) AS max_ts FROM ts_data_minute "
            + "  WHERE station_id = #{stationId} AND ts >= #{since} GROUP BY factor_code"
            + ") x ON x.factor_code = m.factor_code AND x.max_ts = m.ts "
            + "WHERE m.station_id = #{stationId} AND m.ts >= #{since} "
            + "ORDER BY m.factor_code")
    List<TsDataMinute> selectLatestPerFactor(@Param("stationId") Long stationId,
                                             @Param("since") LocalDateTime since);

    /** 跨站房最新的 N 条因子值，用于工作台"站房实时值"；必须带 tenant_id 防跨租户 */
    @Select("SELECT m.station_id, m.factor_code, m.ts, m.`value`, m.dev_flag, m.quality "
            + "FROM ts_data_minute m JOIN ("
            + "  SELECT station_id, factor_code, MAX(ts) AS max_ts FROM ts_data_minute "
            + "  WHERE ts >= #{since} AND tenant_id = #{tenantId} GROUP BY station_id, factor_code"
            + ") x ON x.station_id = m.station_id AND x.factor_code = m.factor_code AND x.max_ts = m.ts "
            + "WHERE m.ts >= #{since} AND m.tenant_id = #{tenantId} "
            + "ORDER BY m.ts DESC LIMIT #{limit}")
    List<TsDataMinute> selectLatestAll(@Param("since") LocalDateTime since,
                                       @Param("tenantId") Long tenantId,
                                       @Param("limit") int limit);

    /**
     * 小时聚合的原料：一条 SQL 按 (小时, 站房, 因子) 汇总区间内的分钟数据。
     *
     * 补传会一次带来多小时的乱序数据，按小时分组后逐个幂等覆盖，
     * 比"每个小时查一次"少 N 倍往返。
     *
     * NOT EXISTS 排除停运区间内的分钟：附录 G 要求停运数据不参与统计。
     * 这些分钟既不算有效也不算缺数，直接从分母里拿掉。
     */
    @Select("<script>"
            + "SELECT CAST(DATE_FORMAT(m.ts, '%Y-%m-%d %H:00:00') AS DATETIME) AS hour_ts, "
            + "m.tenant_id, m.station_id, m.factor_code, COUNT(*) AS total_cnt, "
            + "SUM(CASE WHEN m.quality = 1 THEN 1 ELSE 0 END) AS valid_cnt, "
            + "AVG(CASE WHEN m.quality = 1 THEN m.`value` END) AS avg_valid, "
            + "MAX(CASE WHEN m.quality = 1 THEN m.`value` END) AS max_valid, "
            + "MIN(CASE WHEN m.quality = 1 THEN m.`value` END) AS min_valid, "
            + "AVG(m.`value`) AS avg_all, MAX(m.`value`) AS max_all, MIN(m.`value`) AS min_all, "
            + "SUM(CASE WHEN m.dev_flag = 'D' THEN 1 ELSE 0 END) AS d_cnt, "
            + "SUM(CASE WHEN m.dev_flag = 'C' THEN 1 ELSE 0 END) AS c_cnt, "
            + "SUM(CASE WHEN m.dev_flag = 'M' THEN 1 ELSE 0 END) AS m_cnt, "
            + "SUM(CASE WHEN m.dev_flag = 'T' THEN 1 ELSE 0 END) AS t_cnt, "
            + "SUM(CASE WHEN m.dev_flag = 'Td' THEN 1 ELSE 0 END) AS td_cnt, "
            + "SUM(CASE WHEN m.dev_flag IS NULL OR m.dev_flag = '' THEN 1 ELSE 0 END) AS no_flag_cnt "
            + "FROM ts_data_minute m "
            + "WHERE m.ts &gt;= #{from} AND m.ts &lt; #{to} "
            + "<if test='stationId != null'> AND m.station_id = #{stationId} </if>"
            + "AND NOT EXISTS ("
            + "  SELECT 1 FROM md_station_stop st WHERE st.station_id = m.station_id "
            + "  AND m.ts &gt;= st.begin_at AND (st.end_at IS NULL OR m.ts &lt; st.end_at)"
            + ") "
            + "GROUP BY hour_ts, m.tenant_id, m.station_id, m.factor_code"
            + "</script>")
    List<MinuteAggRow> aggregateByHour(@Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to,
                                       @Param("stationId") Long stationId);

    /**
     * 超标判定原料：只取"有效且超限值"的分钟。
     *
     * 排除 D/C/M/T 标记（附录 G 口径：只有 N 参与超标统计）与停运时段，
     * 限值取该站该因子的执行限值，没有配限值的因子不参与。
     */
    @Select("SELECT m.station_id, m.factor_code, m.ts, m.`value`, f.std_limit AS threshold "
            + "FROM ts_data_minute m "
            + "JOIN md_station_factor f ON f.station_id = m.station_id "
            + "  AND f.factor_code = m.factor_code AND f.deleted = 0 "
            + "WHERE m.tenant_id = #{tenantId} AND m.ts >= #{from} AND m.ts < #{to} "
            + "AND m.dev_flag = 'N' AND m.`value` IS NOT NULL "
            + "AND f.std_limit IS NOT NULL AND m.`value` > f.std_limit "
            + "AND NOT EXISTS ("
            + "  SELECT 1 FROM md_station_stop st WHERE st.station_id = m.station_id "
            + "  AND m.ts >= st.begin_at AND (st.end_at IS NULL OR m.ts < st.end_at)"
            + ") "
            + "ORDER BY m.station_id, m.factor_code, m.ts")
    List<AlarmSampleRow> selectOverLimit(@Param("tenantId") Long tenantId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    /**
     * 恒值判定原料：每个 (站房, 因子) 取最近 N 条有效分钟，按时间正序返回。
     * 窗口函数在库侧先裁到 N 条，避免把整段时间的数据拉进内存。
     */
    @Select("SELECT t.station_id, t.factor_code, t.ts, t.`value` FROM ("
            + "  SELECT m.station_id, m.factor_code, m.ts, m.`value`, "
            + "         ROW_NUMBER() OVER (PARTITION BY m.station_id, m.factor_code ORDER BY m.ts DESC) AS rn "
            + "  FROM ts_data_minute m "
            + "  WHERE m.tenant_id = #{tenantId} AND m.ts >= #{from} AND m.ts < #{to} "
            + "  AND m.dev_flag = 'N' AND m.`value` IS NOT NULL "
            + "  AND NOT EXISTS ("
            + "    SELECT 1 FROM md_station_stop st WHERE st.station_id = m.station_id "
            + "    AND m.ts >= st.begin_at AND (st.end_at IS NULL OR m.ts < st.end_at)"
            + "  )"
            + ") t WHERE t.rn <= #{perSeries} "
            + "ORDER BY t.station_id, t.factor_code, t.ts")
    List<AlarmSampleRow> selectRecentPoints(@Param("tenantId") Long tenantId,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to,
                                            @Param("perSeries") int perSeries);

    /** 缺数判定原料：每个 (站房, 因子) 最后一次上报时间 */
    @Select("SELECT station_id, factor_code, MAX(ts) AS last_ts FROM ts_data_minute "
            + "WHERE tenant_id = #{tenantId} AND ts >= #{since} "
            + "GROUP BY station_id, factor_code")
    List<LastPointRow> selectLastPoints(@Param("tenantId") Long tenantId,
                                        @Param("since") LocalDateTime since);

    /** 标记 ⇄ 工单核验：异常标记时段的起止 */
    @Select("SELECT m.station_id, m.dev_flag, MIN(m.ts) AS min_ts, MAX(m.ts) AS max_ts "
            + "FROM ts_data_minute m "
            + "WHERE m.tenant_id = #{tenantId} AND m.ts >= #{from} AND m.ts < #{to} "
            + "AND m.dev_flag IN ('C', 'M', 'D', 'Td') "
            + "AND NOT EXISTS ("
            + "  SELECT 1 FROM md_station_stop st WHERE st.station_id = m.station_id "
            + "  AND m.ts >= st.begin_at AND (st.end_at IS NULL OR m.ts < st.end_at)"
            + ") "
            + "GROUP BY m.station_id, m.dev_flag")
    List<FlaggedRow> selectFlagged(@Param("tenantId") Long tenantId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    /** 指定站房时段内某类标记是否存在（BR-06 漏标核验） */
    @Select("SELECT COUNT(1) FROM ts_data_minute WHERE station_id = #{stationId} "
            + "AND ts >= #{from} AND ts < #{to} AND dev_flag = #{devFlag}")
    int countFlagged(@Param("stationId") Long stationId,
                     @Param("from") LocalDateTime from,
                     @Param("to") LocalDateTime to,
                     @Param("devFlag") String devFlag);

    /** 区间内的分钟有效率（工作台 KPI），按租户过滤，同样剔除停运区间 */
    @Select("SELECT COUNT(*) AS total_cnt, COALESCE(SUM(CASE WHEN m.quality = 1 THEN 1 ELSE 0 END), 0) AS valid_cnt "
            + "FROM ts_data_minute m "
            + "WHERE m.ts >= #{from} AND m.ts < #{to} AND m.tenant_id = #{tenantId} "
            + "AND NOT EXISTS ("
            + "  SELECT 1 FROM md_station_stop st WHERE st.station_id = m.station_id "
            + "  AND m.ts >= st.begin_at AND (st.end_at IS NULL OR m.ts < st.end_at)"
            + ")")
    CountStat statRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                        @Param("tenantId") Long tenantId);
}
