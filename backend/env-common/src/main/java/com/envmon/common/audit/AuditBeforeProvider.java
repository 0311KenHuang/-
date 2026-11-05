package com.envmon.common.audit;

/**
 * 审计"变更前快照"提供者。
 *
 * 为什么需要它：@Audit 挂在 Controller 上，切面在方法执行后才知道结果，
 * 那时数据库里已经是新值了——不改动业务代码就拿不到 before。
 * 这里把"怎么取旧值"交给各业务模块实现（它知道实体和 id 的关系），
 * 切面只负责在方法执行前调用、脱敏、截断、落库。
 *
 * 实现约束：
 *   - 必须是只读查询，不能有副作用（切面跑在业务方法之前，抛错会打断业务）；
 *   - 拿不到就返回 null（例如 LOGIN/EXPORT 这类没有前置状态的动作）。
 */
public interface AuditBeforeProvider {

    /**
     * @param module 模块名，对应 {@link Audit#module()}
     * @param action 动作，对应 {@link Audit#action()}
     * @param args   业务方法入参
     * @return 变更前快照；null 表示该动作没有前置状态
     */
    Object snapshot(String module, String action, Object[] args);
}
