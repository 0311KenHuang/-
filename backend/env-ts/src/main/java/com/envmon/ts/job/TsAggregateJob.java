package com.envmon.ts.job;

import com.envmon.common.job.RedisJobLock;
import com.envmon.ts.calc.TsProperties;
import com.envmon.ts.service.TsAggregateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

/**
 * 附录 G 定时聚合。
 *
 * 设计取舍：
 *   - 不在接入层同步算小时值：设备应答不能被慢 SQL 拖住，
 *     且 README 的约定是"接入只做解析落库，计算交给下游"；
 *   - 小时任务每 10 分钟滚动重算最近 N 小时，补传/乱序晚到也能自动纠正；
 *   - 日任务次日 00:30 重算前两天，兜住跨天补传。
 *
 * 只在常驻 Web 进程开启（env-ingest 的配置里 ts.job.enabled=false），
 * 避免接入进程重复跑聚合。
 *
 * 分布式锁：多实例 Web 部署时只有拿到 Redis 锁的实例执行，
 * Redis 未配置（optional 依赖，env-ingest 场景）时直接执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ts.job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TsAggregateJob {

    /** 锁 TTL 略大于任务最长耗时（小时任务扫 2 小时窗口） */
    private static final Duration LOCK_TTL = Duration.ofMinutes(8);

    private final TsAggregateService aggregateService;
    private final TsProperties props;
    /** optional 依赖：env-api 有 Redis，env-ingest 没有 → 拿不到时直接执行 */
    private final ObjectProvider<StringRedisTemplate> redisProvider;

    /** 启动后补算最近若干小时，升级部署后不用等下一个整点 */
    @Value("${ts.job.catch-up-hours:24}")
    private int catchUpHours;

    @Scheduled(cron = "${ts.job.hour-cron:0 5,15,25,35,45,55 * * * ?}", zone = "Asia/Shanghai")
    public void hourJob() {
        RedisJobLock.runWithLock(redisProvider.getIfAvailable(), "ts-hour", LOCK_TTL, () -> {
            try {
                int n = aggregateService.aggregateRecent(props.getJobHourWindowHours());
                if (n > 0) {
                    log.info("小时值滚动重算：{} 条", n);
                }
            } catch (Exception e) {
                log.error("小时值任务失败: {}", e.getMessage(), e);
            }
        });
    }

    @Scheduled(cron = "${ts.job.day-cron:0 30 0 * * ?}", zone = "Asia/Shanghai")
    public void dayJob() {
        RedisJobLock.runWithLock(redisProvider.getIfAvailable(), "ts-day", LOCK_TTL, () -> {
            try {
                LocalDate today = LocalDate.now();
                int n = aggregateService.aggregateDays(today.minusDays(2), today);
                log.info("日数据重算：{} 条", n);
            } catch (Exception e) {
                log.error("日数据任务失败: {}", e.getMessage(), e);
            }
        });
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUp() {
        if (catchUpHours <= 0) {
            return;
        }
        RedisJobLock.runWithLock(redisProvider.getIfAvailable(), "ts-catchup",
                Duration.ofMinutes(15), () -> {
                    try {
                        int n = aggregateService.aggregateRecent(catchUpHours);
                        log.info("启动补算最近 {} 小时：{} 条", catchUpHours, n);
                    } catch (Exception e) {
                        log.warn("启动补算失败: {}", e.getMessage());
                    }
                });
    }
}
