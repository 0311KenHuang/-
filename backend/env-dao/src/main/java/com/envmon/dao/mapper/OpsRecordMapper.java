package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.OpsRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OpsRecordMapper extends BaseMapper<OpsRecord> {

    /** 与 [from, to) 时间重叠的已审核记录（标记 ⇄ 工单核验） */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM ops_record WHERE tenant_id = #{tenantId} AND station_id = #{stationId} "
            + "AND status = 'APPROVED' AND deleted = 0 AND begin_time <= #{to} "
            + "AND (end_time IS NULL OR end_time >= #{from})")
    List<OpsRecord> selectApprovedOverlap(@Param("tenantId") Long tenantId,
                                          @Param("stationId") Long stationId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    /** 时间区间内已审核的校准记录（漏标核验） */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM ops_record WHERE tenant_id = #{tenantId} AND status = 'APPROVED' "
            + "AND deleted = 0 AND begin_time >= #{from} AND begin_time < #{to} "
            + "AND (ops_type LIKE '%校准%' OR ops_type LIKE '%CALIB%')")
    List<OpsRecord> selectApprovedCalibration(@Param("tenantId") Long tenantId,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);
}
