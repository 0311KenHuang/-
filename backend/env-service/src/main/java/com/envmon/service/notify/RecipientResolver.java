package com.envmon.service.notify;

import com.envmon.common.context.TenantContextHolder;
import com.envmon.dao.entity.MdStation;
import com.envmon.dao.entity.MsgDuty;
import com.envmon.dao.entity.SysUser;
import com.envmon.dao.mapper.MdStationMapper;
import com.envmon.dao.mapper.MsgDutyMapper;
import com.envmon.dao.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 接收人解析顺序（docs/04 §6.1）：
 *   责任工程师 → 当日值班人 → 角色兜底（ORG → MGR → SUP）。
 * 升级消息直接走角色兜底，找主管而不是继续打扰一线工程师。
 */
@Service
@RequiredArgsConstructor
public class RecipientResolver {

    private final MdStationMapper stationMapper;
    private final SysUserMapper userMapper;
    private final MsgDutyMapper dutyMapper;
    private final NotifyProperties props;

    public Recipient resolve(Long stationId, int stage) {
        Long tenantId = TenantContextHolder.tenantId();
        MdStation station = stationId == null ? null : stationMapper.selectById(stationId);

        if (stage == 0 && station != null) {
            if (station.getOpsEngineerId() != null) {
                SysUser user = userMapper.selectById(station.getOpsEngineerId());
                if (user != null) {
                    return fromUser(user, "ENGINEER");
                }
            }
            if (station.getOpsOrgId() != null) {
                String shift = NotifyPlanner.isDay(LocalDateTime.now().getHour(), props) ? "DAY" : "NIGHT";
                MsgDuty duty = dutyMapper.selectDuty(tenantId, station.getOpsOrgId(), LocalDate.now(), shift);
                if (duty != null) {
                    if (duty.getUserId() != null) {
                        SysUser user = userMapper.selectById(duty.getUserId());
                        if (user != null) {
                            return fromUser(user, "DUTY");
                        }
                    }
                    Recipient r = new Recipient();
                    r.setPhone(duty.getPhone());
                    r.setName("值班人");
                    r.setSource("DUTY");
                    return r;
                }
            }
        }

        List<SysUser> fallback = userMapper.selectRoleFallback(tenantId);
        if (fallback != null && !fallback.isEmpty()) {
            return fromUser(fallback.get(0), "ROLE");
        }
        Recipient unknown = new Recipient();
        unknown.setName("未解析到责任人");
        unknown.setSource("UNKNOWN");
        return unknown;
    }

    private Recipient fromUser(SysUser user, String source) {
        Recipient r = new Recipient();
        r.setUserId(user.getId());
        r.setName(user.getRealName() == null ? user.getUsername() : user.getRealName());
        r.setPhone(user.getPhone());
        r.setSource(source);
        return r;
    }
}
