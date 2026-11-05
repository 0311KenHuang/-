package com.envmon.service.alarm;

import com.envmon.common.job.RedisJobLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 报警扫描任务：每 5 分钟一轮。
 *
 * 只在常驻 Web 进程启用（env-ingest 不含 env-service），
 * 扫描窗口比周期略宽，避免两次扫描之间漏掉两三分钟的数据。
 *
 * 分布式锁：多实例部署时只有拿到锁的实例执行本轮，
 * 拿不到的直接跳过（skip-fast），下一轮自然继续；Redis 不可用时退化为各实例都执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "alarm.job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AlarmJob {

    /** 锁 TTL 略大于一轮扫描的最长耗时 */
    private static final Duration LOCK_TTL = Duration.ofMinutes(4);

    private final AlarmEngine alarmEngine;
    private final StringRedisTemplate redis;

    @Scheduled(cron = "${alarm.job.cron:0 2/5 * * * ?}", zone = "Asia/Shanghai")
    public void scan() {
        RedisJobLock.runWithLock(redis, "alarm-scan", LOCK_TTL, () -> {
            try {
                alarmEngine.scanAllTenants();
            } catch (Exception e) {
                log.error("报警扫描任务失败: {}", e.getMessage(), e);
            }
        });
    }
}
