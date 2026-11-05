import {
  CallHandler,
  ExecutionContext,
  Injectable,
  NestInterceptor,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { Observable, tap } from 'rxjs';
import { AUDIT_KEY, AuditMeta } from '../decorators';
import { AuditService } from '../audit/audit.service';

/** 仅在标注了 @Audit(module, action) 的接口上写审计 */
@Injectable()
export class AuditInterceptor implements NestInterceptor {
  constructor(
    private readonly reflector: Reflector,
    private readonly audit: AuditService,
  ) {}

  intercept(context: ExecutionContext, next: CallHandler): Observable<unknown> {
    const meta = this.reflector.getAllAndOverride<AuditMeta>(AUDIT_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);
    if (!meta) return next.handle();

    const req = context.switchToHttp().getRequest();
    return next.handle().pipe(
      tap((result) => {
        this.audit.record({
          module: meta.module,
          action: meta.action,
          bizId: (result as { id?: number })?.id ?? req.params?.id ?? null,
          after: req.body,
          ip: req.ip,
          ua: req.headers?.['user-agent'] ?? null,
        });
      }),
    );
  }
}
