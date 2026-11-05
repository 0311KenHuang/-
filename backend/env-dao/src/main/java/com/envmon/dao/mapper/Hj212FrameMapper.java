package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.Hj212Frame;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface Hj212FrameMapper extends BaseMapper<Hj212Frame> {

    @Select("SELECT COUNT(1) FROM hj212_frame WHERE received_at >= #{from}")
    long countSince(@Param("from") LocalDateTime from);

    /**
     * 重算用：取出指定租户、指定时间段内可解析的分钟类报文。
     * hj212_frame 没有 tenant_id，必须 join md_station 限定租户，
     * 否则补算会跨租户读到别人家的原始报文。
     */
    @Select("SELECT f.* FROM hj212_frame f JOIN md_station s ON s.id = f.station_id "
            + "WHERE s.tenant_id = #{tenantId} AND s.deleted = 0 "
            + "AND f.crc_ok = 1 AND f.station_id IS NOT NULL "
            + "AND f.cn IN ('2011', '2051', '2041') "
            + "AND f.received_at >= #{from} AND f.received_at < #{to} "
            + "AND f.id > #{afterId} "
            + "ORDER BY f.id LIMIT #{limit}")
    List<Hj212Frame> selectReplayable(@Param("tenantId") Long tenantId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to,
                                      @Param("afterId") long afterId,
                                      @Param("limit") int limit);
}
