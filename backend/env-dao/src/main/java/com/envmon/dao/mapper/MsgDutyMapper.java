package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.MsgDuty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface MsgDutyMapper extends BaseMapper<MsgDuty> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM msg_duty WHERE tenant_id = #{tenantId} AND org_id = #{orgId} "
            + "AND duty_date = #{date} AND shift = #{shift} ORDER BY id LIMIT 1")
    MsgDuty selectDuty(@Param("tenantId") Long tenantId,
                       @Param("orgId") Long orgId,
                       @Param("date") LocalDate date,
                       @Param("shift") String shift);
}
