package com.envmon.common.job;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * 定时任务 Redis 分布式锁（SET NX EX + 比对释放）。
 *
 * 放在 env-common 是因为 env-service（报警/消息/超期任务）和
 * env-ts（时序聚合任务）都要用；Redis 依赖声明为 optional，
 * 不会传染给不配置 Redis 的进程（env-ingest）。
 *
 * 设计取向（MVP 级，够用且零新依赖）：
 * - SET key holder NX EX：拿不到锁直接跳过本轮（skip-fast，不等），
 *   定时任务下一轮自然会再跑，丢一轮无害
 * - 释放用 Lua 比对 holder，避免误删别人的锁
 * - 锁自带 TTL，持锁进程崩溃后锁自动过期，不会死锁
 * - Redis 不可用时 fail-open（照常执行）：单实例部署没有 Redis 也能跑
 */
public final class RedisJobLock {

    private static final String PREFIX = "job:lock:";

    private RedisJobLock() {
    }

    /**
     * 尝试执行受锁保护的任务体。
     *
     * @param redis   Redis 模板（可空，空则直接执行）
     * @param jobName 任务名（锁 key 的一部分，需全局唯一）
     * @param ttl     锁超时：应略大于任务最长耗时，任务没跑完锁先过期会出现双跑
     * @param body    任务体
     * @return true=本次执行了任务体；false=其他实例持有锁，本轮跳过
     */
    public static boolean runWithLock(StringRedisTemplate redis, String jobName,
                                      Duration ttl, Runnable body) {
        if (redis == null) {
            body.run();
            return true;
        }
        String key = PREFIX + jobName;
        String lockHolder = UUID.randomUUID().toString();
        Boolean ok;
        try {
            ok = redis.opsForValue().setIfAbsent(key, lockHolder, ttl);
        } catch (Exception e) {
            // Redis 故障时 fail-open：照常执行，多实例部署退化为可能重复跑
            body.run();
            return true;
        }
        if (!Boolean.TRUE.equals(ok)) {
            return false;
        }
        try {
            body.run();
            return true;
        } finally {
            release(redis, key, lockHolder);
        }
    }

    /** Lua 比对 holder 再删，避免释放了别的实例刚拿到的锁 */
    private static void release(StringRedisTemplate redis, String key, String lockHolder) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<Long>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] then "
                            + "return redis.call('del', KEYS[1]) else return 0 end",
                    Long.class);
            redis.execute(script, Collections.singletonList(key), lockHolder);
        } catch (Exception ignored) {
            // 释放失败无所谓：锁会按 TTL 自动过期
        }
    }
}
