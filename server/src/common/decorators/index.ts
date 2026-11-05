import {
  createParamDecorator,
  ExecutionContext,
  SetMetadata,
} from '@nestjs/common';
import type { RequestContext } from '../context/request.context';

export const IS_PUBLIC_KEY = 'isPublic';
/** 标记接口无需登录 */
export const Public = () => SetMetadata(IS_PUBLIC_KEY, true);

export const ROLES_KEY = 'roles';
/** 允许访问的角色，如 @Roles('REG', 'MGR') */
export const Roles = (...roles: string[]) => SetMetadata(ROLES_KEY, roles);

export const AUDIT_KEY = 'audit';
export interface AuditMeta {
  module: string;
  action: string;
}
/** 标记需要写审计的操作，如 @Audit('station', 'CREATE') */
export const Audit = (module: string, action: string) =>
  SetMetadata(AUDIT_KEY, { module, action } as AuditMeta);

/** 注入当前登录用户上下文 */
export const CurrentUser = createParamDecorator(
  (_data: unknown, ctx: ExecutionContext): RequestContext => {
    const req = ctx.switchToHttp().getRequest();
    return req.ctx as RequestContext;
  },
);
