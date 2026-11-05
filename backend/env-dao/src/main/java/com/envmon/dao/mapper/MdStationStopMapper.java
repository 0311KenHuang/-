package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.MdStationStop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 停运区间读写。
 *
 * 表在租户插件忽略名单里：聚合任务没有登录态，不能依赖插件补 tenant_id。
 * 调用方必须先按站房做租户校验，再按 station_id 读写。
 */
@Mapper
public interface MdStationStopMapper extends BaseMapper<MdStationStop> {

    /** 关闭该站房所有未闭合区间，正常只会有一条；多条说明历史数据有异常，一并收口 */
    @Update("UPDATE md_station_stop SET end_at = #{endAt}, updated_at = NOW() "
            + "WHERE station_id = #{stationId} AND end_at IS NULL")
    int closeOpen(@Param("stationId") Long stationId, @Param("endAt") LocalDateTime endAt);

    @Select("SELECT * FROM md_station_stop WHERE station_id = #{stationId} AND end_at IS NULL "
            + "ORDER BY begin_at DESC LIMIT 1")
    MdStationStop selectOpen(@Param("stationId") Long stationId);

    /** 与 [from, to) 有交集的区间 */
    @Select("SELECT * FROM md_station_stop WHERE station_id = #{stationId} "
            + "AND begin_at < #{to} AND (end_at IS NULL OR end_at > #{from}) "
            + "ORDER BY begin_at DESC")
    List<MdStationStop> selectOverlap(@Param("stationId") Long stationId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    /** 当前仍在停运的站房：报警引擎据此跳过停运站 */
    @Select("SELECT * FROM md_station_stop WHERE tenant_id = #{tenantId} AND end_at IS NULL")
    List<MdStationStop> selectOpenByTenant(@Param("tenantId") Long tenantId);

    /** 区间内的停运小时数，区间跨天时按交集裁剪 */
    @Select("SELECT COALESCE(SUM(TIMESTAMPDIFF(SECOND, "
            + "GREATEST(begin_at, #{from}), LEAST(COALESCE(end_at, #{to}), #{to})"
            + ")) / 3600, 0) "
            + "FROM md_station_stop WHERE station_id = #{stationId} "
            + "AND begin_at < #{to} AND (end_at IS NULL OR end_at > #{from})")
    BigDecimal stoppedHours(@Param("stationId") Long stationId,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to);
}
