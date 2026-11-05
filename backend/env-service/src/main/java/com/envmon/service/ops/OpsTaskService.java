package com.envmon.service.ops;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.envmon.common.api.PageResult;
import com.envmon.common.context.TenantContext;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.common.exception.BizException;
import com.envmon.common.exception.ErrorCode;
import com.envmon.dao.entity.AlmEvent;
import com.envmon.dao.entity.MdStation;
import com.envmon.dao.entity.OpsCheckin;
import com.envmon.dao.entity.OpsTask;
import com.envmon.dao.entity.OpsTaskLog;
import com.envmon.dao.entity.SysUser;
import com.envmon.dao.mapper.AlmEventMapper;
import com.envmon.dao.mapper.MdStationMapper;
import com.envmon.dao.mapper.OpsCheckinMapper;
import com.envmon.dao.mapper.OpsTaskLogMapper;
import com.envmon.dao.mapper.OpsTaskMapper;
import com.envmon.dao.mapper.SysUserMapper;
import com.envmon.service.datascope.DataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 任务流转：接单 → 到场（电子围栏）→ 提交，报警可直接转工单 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpsTaskService {

    private static final int FENCE_METERS = 300;

    private final OpsTaskMapper taskMapper;
    private final OpsTaskLogMapper logMapper;
    private final MdStationMapper stationMapper;
    private final SysUserMapper userMapper;
    private final OpsCheckinMapper checkinMapper;
    private final AlmEventMapper eventMapper;
    private final OpsNoGenerator noGenerator;
    private final DataScopeService dataScopeService;

    public PageResult<OpsView.TaskView> page(OpsRequest.TaskQuery q) {
        int pageNo = q.getPage() <= 0 ? 1 : q.getPage();
        int size = q.getPageSize() <= 0 ? 20 : Math.min(q.getPageSize(), 200);
        LambdaQueryWrapper<OpsTask> w = new LambdaQueryWrapper<OpsTask>();
        // dataScope 落地：
        //   SELF —— 工程师只看派给自己的任务（任务的天然归属是 assignee，不用站房集合）
        //   其余 —— 按可见站房集合过滤（null = ALL 不限制）
        TenantContext ctx = TenantContextHolder.get();
        String scope = ctx == null || ctx.getDataScope() == null ? "SELF" : ctx.getDataScope();
        if ("SELF".equals(scope)) {
            w.eq(OpsTask::getAssigneeId, ctx == null ? null : ctx.getUserId());
        } else {
            List<Long> visible = dataScopeService.visibleStationIds();
            if (visible != null) {
                if (visible.isEmpty()) {
                    return PageResult.of(java.util.Collections.emptyList(), 0, pageNo, size);
                }
                w.in(OpsTask::getStationId, visible);
            }
        }
        if (StringUtils.isNotBlank(q.getStatus())) {
            w.eq(OpsTask::getStatus, q.getStatus().trim());
        }
        if (q.getStationId() != null) {
            w.eq(OpsTask::getStationId, q.getStationId());
        }
        if (q.getAssigneeId() != null) {
            w.eq(OpsTask::getAssigneeId, q.getAssigneeId());
        }
        if (StringUtils.isNotBlank(q.getSource())) {
            w.eq(OpsTask::getSource, q.getSource().trim());
        }
        w.orderByDesc(OpsTask::getId);
        Page<OpsTask> p = taskMapper.selectPage(new Page<OpsTask>(pageNo, size), w);
        return PageResult.of(toViews(p.getRecords()), p.getTotal(), pageNo, size);
    }

    public OpsTask detail(Long id) {
        return require(id);
    }

    /** 状态流转，每次都写 ops_task_log，到场需通过 300 米围栏 */
    @Transactional(rollbackFor = Exception.class)
    public OpsTask act(Long id, OpsRequest.TaskAction req) {
        OpsTask task = require(id);
        String action = req.getAction() == null ? "" : req.getAction().trim().toUpperCase();
        String next = OpsStateMachine.next(task.getStatus(), action);
        LocalDateTime now = LocalDateTime.now();
        Long userId = TenantContextHolder.userId();
        Long tenantId = TenantContextHolder.tenantId();

        if ("ACCEPT".equals(action)) {
            task.setAcceptAt(now);
        } else if ("ARRIVE".equals(action)) {
            checkIn(task, req, now, userId, tenantId);
            task.setArriveAt(now);
        } else if ("SUBMIT".equals(action)) {
            task.setFinishAt(now);
        } else if ("TRANSFER".equals(action)) {
            if (req.getAssigneeId() == null) {
                throw BizException.of(ErrorCode.BAD_REQUEST, "改派必须指定新责任人");
            }
            task.setAssigneeId(req.getAssigneeId());
            task.setDispatchAt(now);
            task.setAcceptAt(null);
            task.setArriveAt(null);
            task.setFinishAt(null);
        } else if ("RETURN".equals(action)) {
            if (StringUtils.isBlank(req.getRemark())) {
                throw BizException.of(ErrorCode.BAD_REQUEST, "退回必须填写原因");
            }
        }

        task.setStatus(next);
        taskMapper.updateById(task);
        writeLog(task.getId(), action, userId, req.getRemark(), tenantId);
        return task;
    }

    /** 报警一键转工单：一个报警只允许生成一张任务 */
    @Transactional(rollbackFor = Exception.class)
    public OpsTask createFromAlarm(Long eventId, Long assigneeId, LocalDateTime deadline) {
        AlmEvent event = eventId == null ? null : eventMapper.selectById(eventId);
        if (event == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "报警事件不存在");
        }
        if (event.getRelatedTaskId() != null) {
            OpsTask existing = taskMapper.selectById(event.getRelatedTaskId());
            if (existing != null) {
                return existing;
            }
        }
        MdStation station = stationMapper.selectById(event.getStationId());
        Long assignee = assigneeId;
        if (assignee == null && station != null) {
            assignee = station.getOpsEngineerId();
        }
        if (assignee == null) {
            assignee = TenantContextHolder.userId();
        }
        LocalDateTime now = LocalDateTime.now();

        OpsTask task = new OpsTask();
        task.setTenantId(event.getTenantId());
        task.setTaskNo(noGenerator.next("TK"));
        task.setStationId(event.getStationId());
        task.setOpsType("报警处置");
        task.setSource("ALARM");
        task.setAssigneeId(assignee);
        task.setDispatchAt(now);
        task.setDeadline(deadline == null ? now.plusHours(4) : deadline);
        task.setStatus("DISPATCHED");
        task.setIsOverdue(0);
        task.setDeleted(0);
        taskMapper.insert(task);
        writeLog(task.getId(), "DISPATCH", TenantContextHolder.userId(),
                "来源报警 " + event.getEventNo(), event.getTenantId());

        event.setRelatedTaskId(task.getId());
        eventMapper.updateById(event);
        return task;
    }

    /** 超期打标，定时任务调用；一条 SQL 全租户生效，不读跨租户数据 */
    public int markOverdue() {
        return taskMapper.markOverdue(LocalDateTime.now());
    }

    /** 审核通过后由审核服务调用，收口任务状态 */
    @Transactional(rollbackFor = Exception.class)
    public void closeByAudit(Long taskId, Long auditorId) {
        OpsTask task = require(taskId);
        task.setStatus("CLOSED");
        task.setFinishAt(LocalDateTime.now());
        taskMapper.updateById(task);
        writeLog(task.getId(), "CLOSE", auditorId, "运维记录审核通过", task.getTenantId());
    }

    private void checkIn(OpsTask task, OpsRequest.TaskAction req, LocalDateTime now,
                         Long userId, Long tenantId) {
        MdStation station = stationMapper.selectById(task.getStationId());
        if (req.getLng() == null || req.getLat() == null || station == null
                || StringUtils.isBlank(station.getLng()) || StringUtils.isBlank(station.getLat())) {
            throw BizException.of(ErrorCode.BAD_REQUEST, "到场打卡必须上报经纬度，且站房需配置坐标");
        }
        int distance = GeoUtil.distanceMeters(req.getLng(), req.getLat(),
                Double.parseDouble(station.getLng()), Double.parseDouble(station.getLat()));
        if (distance > FENCE_METERS) {
            throw BizException.of(ErrorCode.BAD_REQUEST,
                    "异地打卡：距站房 " + distance + " 米，超过 " + FENCE_METERS + " 米，不能标记到场");
        }
        OpsCheckin checkin = new OpsCheckin();
        checkin.setTenantId(tenantId);
        checkin.setTaskId(task.getId());
        checkin.setUserId(userId);
        checkin.setStationId(task.getStationId());
        checkin.setLng(new java.math.BigDecimal(String.valueOf(req.getLng())));
        checkin.setLat(new java.math.BigDecimal(String.valueOf(req.getLat())));
        checkin.setDistanceM(distance);
        checkin.setPhotoKey(req.getPhotoKey());
        checkin.setInAt(now);
        checkin.setIsValid(1);
        checkinMapper.insert(checkin);
    }

    private List<OpsView.TaskView> toViews(List<OpsTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return new ArrayList<OpsView.TaskView>();
        }
        Set<Long> stationIds = new HashSet<Long>();
        Set<Long> userIds = new HashSet<Long>();
        for (OpsTask t : tasks) {
            stationIds.add(t.getStationId());
            userIds.add(t.getAssigneeId());
        }
        Map<Long, String> stationNames = new HashMap<Long, String>();
        if (!stationIds.isEmpty()) {
            for (MdStation s : stationMapper.selectBatchIds(stationIds)) {
                stationNames.put(s.getId(), s.getName());
            }
        }
        Map<Long, String> userNames = new HashMap<Long, String>();
        if (!userIds.isEmpty()) {
            for (SysUser u : userMapper.selectBatchIds(userIds)) {
                userNames.put(u.getId(), u.getRealName() == null ? u.getUsername() : u.getRealName());
            }
        }

        List<OpsView.TaskView> views = new ArrayList<OpsView.TaskView>(tasks.size());
        for (OpsTask t : tasks) {
            OpsView.TaskView v = new OpsView.TaskView();
            v.setId(t.getId());
            v.setTaskNo(t.getTaskNo());
            v.setStationId(t.getStationId());
            v.setStationName(stationNames.get(t.getStationId()));
            v.setOpsType(t.getOpsType());
            v.setSource(t.getSource());
            v.setAssigneeId(t.getAssigneeId());
            v.setAssigneeName(userNames.get(t.getAssigneeId()));
            v.setDispatchAt(t.getDispatchAt());
            v.setAcceptAt(t.getAcceptAt());
            v.setArriveAt(t.getArriveAt());
            v.setFinishAt(t.getFinishAt());
            v.setDeadline(t.getDeadline());
            v.setStatus(t.getStatus());
            v.setIsOverdue(t.getIsOverdue());
            views.add(v);
        }
        return views;
    }

    private void writeLog(Long taskId, String action, Long operator, String remark, Long tenantId) {
        OpsTaskLog entry = new OpsTaskLog();
        entry.setTenantId(tenantId);
        entry.setTaskId(taskId);
        entry.setAction(action);
        entry.setOperator(operator == null ? 0L : operator);
        entry.setRemark(remark);
        logMapper.insert(entry);
    }

    private OpsTask require(Long id) {
        OpsTask task = id == null ? null : taskMapper.selectById(id);
        if (task == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "运维任务不存在");
        }
        return task;
    }
}
