package com.envmon.service.ops;

import com.envmon.common.job.RedisJobLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 任务超期打标：每 5 分钟扫一次 deadline 已过且未终结的任务；多实例下由 Redis 锁去重 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ops.job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpsOverdueJob {

    private static final Duration LOCK_TTL = Duration.ofMinutes(4);

    private final OpsTaskService taskService;
    private final StringRedisTemplate redis;

    @Scheduled(cron = "${ops.job.cron:0 4/5 * * * ?}", zone = "Asia/Shanghai")
    public void scan() {
        RedisJobLock.runWithLock(redis, "ops-overdue", LOCK_TTL, () -> {
            try {
                int n = taskService.markOverdue();
                if (n > 0) {
                    log.info("运维任务超期打标 {} 条", n);
                }
            } catch (Exception e) {
                log.error("任务超期扫描失败: {}", e.getMessage(), e);
            }
        });
    }
}
