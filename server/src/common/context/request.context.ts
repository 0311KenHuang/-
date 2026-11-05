import { AsyncLocalStorage } from 'node:async_hooks';
import { BizException, ErrCode } from '../errors/biz.exception';

/**
 * 请求级上下文。
 *
 * 用 AsyncLocalStorage 而不是给每个方法加参数，是为了让数据权限（tenant_id）
 * 在 Service、Repository、审计、日志里都能直接取到，避免"漏传一个参数就跨租户"。
 */
export interface RequestContext {
  traceId: string;
  tenantId: number;
  userId: number;
  username: string;
  orgId: number | null;
  roles: string[];
  /** 数据范围：ALL 全部 / REGION 本区域 / ORG 本组织 / SELF 仅本人 */
  dataScope: 'ALL' | 'REGION' | 'ORG' | 'SELF';
}

const storage = new AsyncLocalStorage<RequestContext>();

export const Ctx = {
  run<T>(ctx: RequestContext, fn: () => T): T {
    return storage.run(ctx, fn);
  },

  get(): RequestContext | undefined {
    return storage.getStore();
  },

  /** 取当前租户，取不到直接抛错——宁可失败，也不要静默查全表 */
  tenantId(): number {
    const ctx = storage.getStore();
    if (!ctx?.tenantId) {
      throw new BizException(ErrCode.TENANT_MISSING, '缺少租户上下文');
    }
    return ctx.tenantId;
  },

  userId(): number {
    return storage.getStore()?.userId ?? 0;
  },

  traceId(): string {
    return storage.getStore()?.traceId ?? '-';
  },
};
