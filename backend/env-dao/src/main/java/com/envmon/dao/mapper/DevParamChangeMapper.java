package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.DevParamChange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DevParamChangeMapper extends BaseMapper<DevParamChange> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM dev_param_change WHERE tenant_id = #{tenantId} "
            + "AND change_time >= #{from} AND change_time < #{to} "
            + "ORDER BY change_time DESC LIMIT #{limit}")
    List<DevParamChange> selectRecent(@Param("tenantId") Long tenantId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to,
                                      @Param("limit") int limit);

    /** 抑制窗口：同一设备同一参数最近一次变更时间 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT MAX(change_time) FROM dev_param_change WHERE device_id = #{deviceId} "
            + "AND param_code = #{paramCode}")
    LocalDateTime selectLastChangeTime(@Param("deviceId") Long deviceId,
                                       @Param("paramCode") String paramCode);
}
