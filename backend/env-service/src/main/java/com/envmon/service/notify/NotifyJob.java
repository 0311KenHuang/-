package com.envmon.service.notify;

import com.envmon.common.job.RedisJobLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 升级扫描：每 5 分钟检查未认领事件是否到达 T+15min / T+30min；多实例下由 Redis 锁去重 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notify.job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotifyJob {

    private static final Duration LOCK_TTL = Duration.ofMinutes(4);

    private final NotificationService notificationService;
    private final StringRedisTemplate redis;

    @Scheduled(cron = "${notify.job.cron:0 3/5 * * * ?}", zone = "Asia/Shanghai")
    public void escalate() {
        RedisJobLock.runWithLock(redis, "notify-escalate", LOCK_TTL, () -> {
            try {
                notificationService.escalateAll();
            } catch (Exception e) {
                log.error("消息升级任务失败: {}", e.getMessage(), e);
            }
        });
    }
}
