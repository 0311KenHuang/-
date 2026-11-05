package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.DevParamSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DevParamSnapshotMapper extends BaseMapper<DevParamSnapshot> {

    /** 变更检测：同一设备同一参数最后一次上报值 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT param_value FROM dev_param_snapshot WHERE device_id = #{deviceId} "
            + "AND param_code = #{paramCode} ORDER BY collected_at DESC, id DESC LIMIT 1")
    String selectLastValue(@Param("deviceId") Long deviceId, @Param("paramCode") String paramCode);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM dev_param_snapshot WHERE tenant_id = #{tenantId} "
            + "AND station_id = #{stationId} AND collected_at >= #{from} AND collected_at < #{to} "
            + "ORDER BY collected_at DESC LIMIT #{limit}")
    List<DevParamSnapshot> selectByStation(@Param("tenantId") Long tenantId,
                                           @Param("stationId") Long stationId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to,
                                           @Param("limit") int limit);

    /** 每设备每参数的最新一条 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT s.* FROM dev_param_snapshot s JOIN ("
            + "  SELECT device_id, param_code, MAX(id) AS max_id FROM dev_param_snapshot "
            + "  WHERE tenant_id = #{tenantId} AND station_id = #{stationId} GROUP BY device_id, param_code"
            + ") x ON x.max_id = s.id ORDER BY s.param_code")
    List<DevParamSnapshot> selectLatest(@Param("tenantId") Long tenantId,
                                        @Param("stationId") Long stationId);

    /** 工作台用：最近的参数条数 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(1) FROM dev_param_snapshot WHERE tenant_id = #{tenantId} AND collected_at >= #{from}")
    long countSince(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);
}
