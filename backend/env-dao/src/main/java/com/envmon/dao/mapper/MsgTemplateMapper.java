package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.MsgTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MsgTemplateMapper extends BaseMapper<MsgTemplate> {

    /** 定时任务与接口都会调用，显式带 tenant_id，绕开租户插件依赖上下文 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM msg_template WHERE tenant_id = #{tenantId} AND event_type = #{eventType} "
            + "AND channel = #{channel} AND enabled = 1 AND deleted = 0 LIMIT 1")
    MsgTemplate selectTemplate(@Param("tenantId") Long tenantId,
                               @Param("eventType") String eventType,
                               @Param("channel") String channel);
}
