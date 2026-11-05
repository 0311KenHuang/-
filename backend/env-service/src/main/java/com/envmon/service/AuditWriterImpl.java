package com.envmon.service;

import com.envmon.common.audit.AuditLogEntry;
import com.envmon.common.audit.AuditWriter;
import com.envmon.dao.entity.SysAuditLog;
import com.envmon.dao.mapper.SysAuditLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 审计落库实现：只 insert，不提供修改入口。
 *
 * 异步化：请求线程只负责构建 entry（上下文/traceId/入参都在切面里取好了），
 * insert 丢给单线程后台执行器——审计写库慢不再拖慢业务请求。
 * 有界队列（1000）满了直接丢弃并记日志：审计缺失不能反过来打垮业务。
 */
@Slf4j
@Component
public class AuditWriterImpl implements AuditWriter {

    private final SysAuditLogMapper auditLogMapper;

    private final ExecutorService executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1000),
            new ThreadFactory() {
                private final AtomicInteger seq = new AtomicInteger();

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "audit-writer-" + seq.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            });

    public AuditWriterImpl(SysAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public void write(AuditLogEntry entry) {
        try {
            executor.execute(() -> doInsert(entry));
        } catch (Exception e) {
            // 队列满或执行器异常：丢弃本条审计，绝不影响业务
            log.warn("审计任务提交失败（已丢弃）: {}", e.getMessage());
        }
    }

    private void doInsert(AuditLogEntry entry) {
        try {
            SysAuditLog row = new SysAuditLog();
            row.setTenantId(entry.getTenantId() == null ? 0L : entry.getTenantId());
            row.setUserId(entry.getUserId() == null ? 0L : entry.getUserId());
            row.setUserName(entry.getUserName());
            row.setModule(entry.getModule());
            row.setAction(entry.getAction());
            row.setBizId(entry.getBizId());
            row.setBeforeJson(entry.getBeforeJson());
            row.setAfterJson(entry.getAfterJson());
            row.setIp(entry.getIp());
            row.setUa(entry.getUa());
            row.setCreatedAt(LocalDateTime.now());
            auditLogMapper.insert(row);
        } catch (Exception e) {
            log.warn("审计落库失败: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            // 优雅退出：最多等 5 秒把队列里的审计写完
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
