import {
  ArgumentsHost,
  Catch,
  ExceptionFilter,
  HttpException,
  HttpStatus,
  Logger,
} from '@nestjs/common';
import type { Response } from 'express';
import { BizException, ErrCode } from '../errors/biz.exception';
import { Ctx } from '../context/request.context';

@Catch()
export class AllExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger('Exception');

  catch(exception: unknown, host: ArgumentsHost): void {
    const res = host.switchToHttp().getResponse<Response>();
    const traceId = Ctx.traceId();

    let httpStatus = HttpStatus.OK;
    let code: number = ErrCode.INTERNAL;
    let message = '系统繁忙，请稍后重试';

    if (exception instanceof BizException) {
      code = exception.code;
      message = exception.message;
      httpStatus = exception.httpStatus;
    } else if (exception instanceof HttpException) {
      httpStatus = exception.getStatus();
      const body = exception.getResponse();
      message =
        typeof body === 'string'
          ? body
          : ((body as Record<string, unknown>).message as string) || exception.message;
      if (Array.isArray(message)) message = (message as string[]).join('; ');
      code =
        httpStatus === 401
          ? ErrCode.UNAUTHORIZED
          : httpStatus === 403
            ? ErrCode.FORBIDDEN
            : httpStatus === 404
              ? ErrCode.NOT_FOUND
              : ErrCode.BAD_REQUEST;
    } else {
      const err = exception as Error;
      this.logger.error(`[${traceId}] ${err?.message}`, err?.stack);
    }

    res.status(httpStatus).json({ code, message, data: null, traceId });
  }
}
