package com.envmon.service.ops;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.common.exception.BizException;
import com.envmon.common.exception.ErrorCode;
import com.envmon.dao.entity.MdStation;
import com.envmon.dao.entity.OpsPlan;
import com.envmon.dao.entity.OpsPlanItem;
import com.envmon.dao.entity.OpsTask;
import com.envmon.dao.entity.OpsTaskLog;
import com.envmon.dao.mapper.MdStationMapper;
import com.envmon.dao.mapper.OpsPlanItemMapper;
import com.envmon.dao.mapper.OpsPlanMapper;
import com.envmon.dao.mapper.OpsTaskLogMapper;
import com.envmon.dao.mapper.OpsTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 运维计划：月度排期 → 计划明细 → 发布实例化为任务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpsPlanService {

    private static final List<String> DEFAULT_OPS_TYPES = Arrays.asList("巡检", "校准", "试剂更换");

    private final MdStationMapper stationMapper;
    private final OpsPlanMapper planMapper;
    private final OpsPlanItemMapper itemMapper;
    private final OpsTaskMapper taskMapper;
    private final OpsTaskLogMapper taskLogMapper;
    private final OpsNoGenerator noGenerator;

    /** 按"应运维站房"生成月度计划，已排期的站房跳过（幂等） */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generate(OpsRequest.PlanGenerate req) {
        String period = req.getPeriod() == null || !req.getPeriod().matches("\\d{4}-\\d{2}")
                ? YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) : req.getPeriod();
        Long tenantId = TenantContextHolder.tenantId();
        Long userId = TenantContextHolder.userId();

        List<String> opsTypes = req.getOpsTypes() == null || req.getOpsTypes().isEmpty()
                ? DEFAULT_OPS_TYPES : req.getOpsTypes();
        LocalDate firstDay = YearMonth.parse(period).atDay(1);
        LocalDate planDate = req.getPlanDate() == null ? firstDay : req.getPlanDate();

        List<MdStation> stations = stationMapper.selectList(new QueryWrapper<MdStation>()
                .in("biz_status", "NORMAL", "ABNORMAL"));
        Set<Long> already = new HashSet<Long>();
        for (OpsPlan plan : planMapper.selectByPeriod(tenantId, period)) {
            already.add(plan.getStationId());
        }

        int plans = 0;
        int items = 0;
        for (MdStation station : stations) {
            if (already.contains(station.getId())) {
                continue;
            }
            OpsPlan plan = new OpsPlan();
            plan.setTenantId(tenantId);
            plan.setPlanNo("PL-" + period.replace("-", "") + "-" + station.getId());
            plan.setStationId(station.getId());
            plan.setPeriod(period);
            plan.setPlanType("MONTHLY");
            plan.setPlannedDate(planDate);
            plan.setSource("AUTO");
            plan.setStatus("DRAFT");
            plan.setCreatedBy(userId);
            plan.setDeleted(0);
            planMapper.insert(plan);
            plans++;

            int offset = 0;
            for (String opsType : opsTypes) {
                OpsPlanItem item = new OpsPlanItem();
                item.setTenantId(tenantId);
                item.setPlanId(plan.getId());
                item.setOpsType(opsType);
                item.setPlanDate(planDate.plusDays(offset));
                item.setAssigneeId(req.getAssigneeId() == null
                        ? station.getOpsEngineerId() : req.getAssigneeId());
                item.setDone(0);
                itemMapper.insert(item);
                items++;
                offset += 7;
            }
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("period", period);
        result.put("plans", plans);
        result.put("items", items);
        log.info("月度计划生成 {}：新增 {} 份 / {} 条明细", period, plans, items);
        return result;
    }

    public List<OpsPlan> list(String period) {
        String p = period == null || !period.matches("\\d{4}-\\d{2}")
                ? YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) : period;
        return planMapper.selectByPeriod(TenantContextHolder.tenantId(), p);
    }

    public List<OpsPlanItem> items(Long planId) {
        requirePlan(planId);
        return itemMapper.selectList(new QueryWrapper<OpsPlanItem>()
                .eq("plan_id", planId).orderByAsc("plan_date"));
    }

    /** 发布计划：每条未实例化明细生成一张任务单 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> publish(Long planId) {
        OpsPlan plan = requirePlan(planId);
        if ("DONE".equals(plan.getStatus())) {
            throw BizException.of(ErrorCode.CONFLICT, "计划已发布，不能重复下发");
        }
        Long tenantId = TenantContextHolder.tenantId();
        Long userId = TenantContextHolder.userId();
        LocalDateTime now = LocalDateTime.now();

        List<OpsPlanItem> items = itemMapper.selectList(new QueryWrapper<OpsPlanItem>()
                .eq("plan_id", planId).eq("done", 0));
        int created = 0;
        for (OpsPlanItem item : items) {
            if (item.getAssigneeId() == null) {
                throw BizException.of(ErrorCode.BAD_REQUEST,
                        "计划明细 " + item.getId() + " 未指派责任人，不能下发");
            }
            OpsTask task = new OpsTask();
            task.setTenantId(tenantId);
            task.setTaskNo(noGenerator.next("TK"));
            task.setPlanItemId(item.getId());
            task.setStationId(plan.getStationId());
            task.setOpsType(item.getOpsType());
            task.setSource("PLAN");
            task.setAssigneeId(item.getAssigneeId());
            task.setDispatchAt(now);
            task.setDeadline(item.getPlanDate().plusDays(1).atTime(18, 0));
            task.setStatus("DISPATCHED");
            task.setIsOverdue(0);
            task.setDeleted(0);
            taskMapper.insert(task);
            writeLog(task.getId(), "DISPATCH", userId, "来源计划 " + plan.getPlanNo(), tenantId);

            item.setDone(1);
            itemMapper.updateById(item);
            created++;
        }
        plan.setStatus("DONE");
        plan.setApproveBy(userId);
        plan.setApproveAt(now);
        planMapper.updateById(plan);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("planId", planId);
        result.put("tasks", created);
        return result;
    }

    /** BR-01 计划覆盖率 = 已排期站房 / 应运维站房 */
    public OpsView.Coverage coverage(String period) {
        String p = period == null || !period.matches("\\d{4}-\\d{2}")
                ? YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) : period;
        Long tenantId = TenantContextHolder.tenantId();
        List<MdStation> stations = stationMapper.selectList(new QueryWrapper<MdStation>()
                .in("biz_status", "NORMAL", "ABNORMAL"));
        Set<Long> planned = new HashSet<Long>();
        for (OpsPlan plan : planMapper.selectByPeriod(tenantId, p)) {
            planned.add(plan.getStationId());
        }
        OpsView.Coverage c = new OpsView.Coverage();
        c.setPeriod(p);
        c.setTotalStations(stations.size());
        c.setPlannedStations(planned.size());
        c.setCoverageRate(stations.isEmpty() ? 0
                : Math.round(planned.size() * 10000.0 / stations.size()) / 10000.0);
        for (MdStation station : stations) {
            if (!planned.contains(station.getId())) {
                c.getUnplannedStationIds().add(station.getId());
            }
        }
        return c;
    }

    private OpsPlan requirePlan(Long planId) {
        OpsPlan plan = planId == null ? null : planMapper.selectById(planId);
        if (plan == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "运维计划不存在");
        }
        return plan;
    }

    private void writeLog(Long taskId, String action, Long operator, String remark, Long tenantId) {
        OpsTaskLog entry = new OpsTaskLog();
        entry.setTenantId(tenantId);
        entry.setTaskId(taskId);
        entry.setAction(action);
        entry.setOperator(operator == null ? 0L : operator);
        entry.setRemark(remark);
        taskLogMapper.insert(entry);
    }

}
