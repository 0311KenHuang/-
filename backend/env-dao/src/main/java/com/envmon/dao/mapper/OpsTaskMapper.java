package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.OpsTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface OpsTaskMapper extends BaseMapper<OpsTask> {

    /**
     * 超期打标：定时任务无租户上下文，且这是一条全租户的批量标记 SQL，
     * 不读取/返回任何跨租户数据，显式绕开租户插件。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE ops_task SET is_overdue = 1, updated_at = NOW() "
            + "WHERE is_overdue = 0 AND deleted = 0 AND deadline IS NOT NULL AND deadline < #{now} "
            + "AND status NOT IN ('CLOSED', 'RETURNED', 'SUBMITTED')")
    int markOverdue(@Param("now") LocalDateTime now);
}
