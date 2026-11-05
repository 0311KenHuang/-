import {
  CallHandler,
  ExecutionContext,
  Injectable,
  NestInterceptor,
} from '@nestjs/common';
import { Observable } from 'rxjs';
import { Ctx } from '../context/request.context';

/**
 * 把请求上下文放进 AsyncLocalStorage，之后的 Service / Repository 才能拿到 tenantId。
 * 必须排在所有业务拦截器之前（AppModule 中第一个注册）。
 */
@Injectable()
export class ContextInterceptor implements NestInterceptor {
  intercept(context: ExecutionContext, next: CallHandler): Observable<unknown> {
    const req = context.switchToHttp().getRequest();
    if (!req.ctx) return next.handle();
    return Ctx.run(req.ctx, () => next.handle());
  }
}
