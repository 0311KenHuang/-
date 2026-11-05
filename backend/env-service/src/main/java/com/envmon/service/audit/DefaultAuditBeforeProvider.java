package com.envmon.service.audit;

import com.envmon.common.audit.AuditBeforeProvider;
import com.envmon.dao.mapper.AlmEventMapper;
import com.envmon.dao.mapper.MdStationMapper;
import com.envmon.dao.mapper.OpsRecordMapper;
import com.envmon.dao.mapper.OpsTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 审计前置快照：按 module/action 从入参里取主键，回查一次当前值。
 *
 * 覆盖范围按"改了以后无法还原"排序，先补最需要的三类：
 *   station UPDATE / CHANGE_STATUS  —— 站房档案与启停状态，监管核查重点
 *   ops     TASK_ACTION             —— 工单流转，责任认定依据
 *   ops     RECORD_AUDIT            —— 记录审核结论，不可回改
 *   alarm   HANDLE                  —— 报警认领/关闭/误报
 *
 * 这些动作的入参形如 (id, dto)，id 就是业务主键。
 * 查不到的（已被删除或无权限）返回 null——审计少一段快照，但不能因此让业务失败。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultAuditBeforeProvider implements AuditBeforeProvider {

    private final MdStationMapper stationMapper;
    private final OpsTaskMapper taskMapper;
    private final OpsRecordMapper recordMapper;
    private final AlmEventMapper eventMapper;

    @Override
    public Object snapshot(String module, String action, Object[] args) {
        Long id = firstId(args);
        if (id == null) {
            return null;
        }
        if ("station".equals(module)) {
            // CREATE 没有旧值；UPDATE 与 CHANGE_STATUS 都以站房为对象
            return stationMapper.selectById(id);
        }
        if ("ops".equals(module)) {
            if ("TASK_ACTION".equals(action)) {
                return taskMapper.selectById(id);
            }
            if ("RECORD_AUDIT".equals(action)) {
                return recordMapper.selectById(id);
            }
            return null;
        }
        if ("alarm".equals(module) && "HANDLE".equals(action)) {
            return eventMapper.selectById(id);
        }
        return null;
    }

    /** 约定：路径变量 id 是第一个入参 */
    private Long firstId(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        Object first = args[0];
        if (first instanceof Number) {
            return ((Number) first).longValue();
        }
        return null;
    }
}
