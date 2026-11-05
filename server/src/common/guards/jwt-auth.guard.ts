import {
  CanActivate,
  ExecutionContext,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { JwtService } from '@nestjs/jwt';
import { IS_PUBLIC_KEY } from '../decorators';
import type { RequestContext } from '../context/request.context';

export interface JwtPayload {
  sub: number;
  tenantId: number;
  username: string;
  orgId: number | null;
  roles: string[];
  scope: RequestContext['dataScope'];
}

@Injectable()
export class JwtAuthGuard implements CanActivate {
  constructor(
    private readonly reflector: Reflector,
    private readonly jwt: JwtService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const isPublic = this.reflector.getAllAndOverride<boolean>(IS_PUBLIC_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);
    if (isPublic) return true;

    const req = context.switchToHttp().getRequest();
    const raw: string | undefined =
      req.headers?.authorization ?? req.headers?.Authorization;

    if (!raw?.startsWith('Bearer ')) {
      throw new UnauthorizedException('未登录或令牌缺失');
    }

    let payload: JwtPayload;
    try {
      payload = await this.jwt.verifyAsync<JwtPayload>(raw.slice(7));
    } catch {
      throw new UnauthorizedException('令牌无效或已过期');
    }

    req.ctx = {
      traceId: req.headers['x-trace-id'] ?? `${Date.now()}-${payload.sub}`,
      tenantId: payload.tenantId,
      userId: payload.sub,
      username: payload.username,
      orgId: payload.orgId ?? null,
      roles: payload.roles ?? [],
      dataScope: payload.scope ?? 'ORG',
    } satisfies RequestContext;

    return true;
  }
}
