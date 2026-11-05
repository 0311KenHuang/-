import {
  CanActivate,
  ExecutionContext,
  ForbiddenException,
  Injectable,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { IS_PUBLIC_KEY, ROLES_KEY } from '../decorators';

@Injectable()
export class RolesGuard implements CanActivate {
  constructor(private readonly reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const isPublic = this.reflector.getAllAndOverride<boolean>(IS_PUBLIC_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);
    if (isPublic) return true;

    const required = this.reflector.getAllAndOverride<string[]>(ROLES_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);
    if (!required?.length) return true;

    const roles: string[] = context.switchToHttp().getRequest().ctx?.roles ?? [];
    // 超管默认放行，避免每加一个接口都要记得补 SUP
    if (roles.includes('SUP')) return true;

    if (!required.some((r) => roles.includes(r))) {
      throw new ForbiddenException(`需要角色：${required.join(' / ')}`);
    }
    return true;
  }
}
