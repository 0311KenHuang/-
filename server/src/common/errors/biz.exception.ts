/**
 * 业务异常：统一以 { code, message } 返回，HTTP 状态默认 200。
 * 只有认证失败（401）与系统错误（5xx）保留 HTTP 语义。
 */
export class BizException extends Error {
  constructor(
    public readonly code: number,
    message: string,
    public readonly httpStatus: number = 200,
  ) {
    super(message);
    this.name = 'BizException';
  }

  static of(code: number, message: string): BizException {
    return new BizException(code, message);
  }
}

/** 常用错误码：与前端 errorCode 表一一对应 */
export const ErrCode = {
  OK: 0,
  BAD_REQUEST: 40000,
  UNAUTHORIZED: 40100,
  FORBIDDEN: 40300,
  NOT_FOUND: 40400,
  CONFLICT: 40900,
  TENANT_MISSING: 40101,
  DATA_SCOPE_DENIED: 40301,
  INTERNAL: 50000,
} as const;
