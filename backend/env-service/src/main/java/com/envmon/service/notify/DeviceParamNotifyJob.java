package com.envmon.service.notify;

import com.envmon.common.context.TenantContext;
import com.envmon.common.context.TenantContextHolder;
import com.envmon.dao.entity.AlmEvent;
import com.envmon.dao.mapper.AlmEventMapper;
import com.envmon.common.job.RedisJobLock;
import com.envmon.ts.service.DeviceParamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 接入层产生的「工作参数变更」事件在这里补发首报。
 *
 * 接入进程不碰外部通道（避免慢 HTTP 拖住设备应答），API 进程用
 * "事件没有任何发送日志" 作为待发判据，天然幂等。
 * 多实例部署由 Redis 锁去重（拿不到锁直接跳过本轮）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notify.param-job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeviceParamNotifyJob {

    private static final Duration LOCK_TTL = Duration.ofMinutes(4);

    private final AlmEventMapper eventMapper;
    private final NotificationService notificationService;
    private final StringRedisTemplate redis;

    @Scheduled(cron = "${notify.param-job.cron:0 1/5 * * * ?}", zone = "Asia/Shanghai")
    public void scan() {
        RedisJobLock.runWithLock(redis, "device-param-notify", LOCK_TTL, this::doScan);
    }

    private void doScan() {
        TenantContext previous = TenantContextHolder.get();
        List<AlmEvent> events = eventMapper.selectUnnotifiedByRule(DeviceParamService.RULE_CODE, 100);
        int sent = 0;
        try {
            for (AlmEvent event : events == null ? java.util.Collections.<AlmEvent>emptyList() : events) {
                TenantContext ctx = new TenantContext();
                ctx.setTenantId(event.getTenantId());
                ctx.setTraceId(previous == null ? null : previous.getTraceId());
                TenantContextHolder.set(ctx);
                try {
                    notificationService.onCreated(event);
                    sent++;
                } catch (Exception e) {
                    log.error("参数变更事件 {} 通知失败: {}", event.getEventNo(), e.getMessage());
                } finally {
                    TenantContextHolder.clear();
                }
            }
        } finally {
            if (previous != null) {
                TenantContextHolder.set(previous);
            } else {
                TenantContextHolder.clear();
            }
        }
        if (sent > 0) {
            log.info("参数变更首报补发 {} 条", sent);
        }
    }
}
