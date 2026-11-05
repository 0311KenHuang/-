package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.AlmEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AlmEventMapper extends BaseMapper<AlmEvent> {

    /**
     * 接入层产生的事件（如工作参数变更）还没有任何发送日志时，由 API 进程补发首报。
     * 用 NOT EXISTS 判重，不需要额外状态字段；跨租户扫描，调用方按 event.tenant_id 设置上下文。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT e.* FROM alm_event e "
            + "JOIN alm_rule r ON r.id = e.rule_id AND r.rule_code = #{ruleCode} "
            + "WHERE e.status = 'OPEN' AND e.deleted = 0 "
            + "AND NOT EXISTS (SELECT 1 FROM msg_send_log l WHERE l.event_id = e.id) "
            + "ORDER BY e.begin_at LIMIT #{limit}")
    List<AlmEvent> selectUnnotifiedByRule(@Param("ruleCode") String ruleCode,
                                          @Param("limit") int limit);
}
