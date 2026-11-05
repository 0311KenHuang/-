import {
  CallHandler,
  ExecutionContext,
  Injectable,
  NestInterceptor,
} from '@nestjs/common';
import { Observable, map } from 'rxjs';
import { Ctx } from '../context/request.context';

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  traceId: string;
}

/** 统一响应体：{ code, message, data, traceId } */
@Injectable()
export class ResponseInterceptor<T> implements NestInterceptor<T, ApiResponse<T>> {
  intercept(context: ExecutionContext, next: CallHandler<T>): Observable<ApiResponse<T>> {
    return next.handle().pipe(
      map((data) => ({
        code: 0,
        message: 'ok',
        data,
        traceId: Ctx.traceId(),
      })),
    );
  }
}
