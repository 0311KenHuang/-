package com.envmon.common.audit;

/**
 * 审计落库接口。由业务模块实现（避免 common 依赖 dao）。
 * 实现类应使用独立线程池异步写入，且不影响主流程。
 */
public interface AuditWriter {

    void write(AuditLogEntry entry);
}
