import { Injectable, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { SysAuditLog } from '../../database/entities/sys-audit-log.entity';
import { Ctx } from '../context/request.context';

export interface AuditEntry {
  module: string;
  action: string;
  bizId?: string | number | null;
  before?: unknown;
  after?: unknown;
  ip?: string | null;
  ua?: string | null;
}

/** 审计只做一件事：把"谁在什么时候改了什么"记下来。失败不阻塞业务。 */
@Injectable()
export class AuditService {
  private readonly logger = new Logger(AuditService.name);

  constructor(
    @InjectRepository(SysAuditLog)
    private readonly repo: Repository<SysAuditLog>,
  ) {}

  record(entry: AuditEntry): void {
    const ctx = Ctx.get();
    const row = this.repo.create({
      tenantId: ctx?.tenantId ?? 0,
      userId: ctx?.userId ?? 0,
      userName: ctx?.username ?? null,
      module: entry.module,
      action: entry.action,
      bizId: entry.bizId != null ? String(entry.bizId) : null,
      beforeJson: this.sanitize(entry.before),
      afterJson: this.sanitize(entry.after),
      ip: entry.ip ?? null,
      ua: entry.ua ?? null,
    });
    // 故意不 await：审计失败不应该让业务请求失败
    void this.repo.save(row).catch((e: Error) => {
      this.logger.warn(`审计写入失败: ${e.message}`);
    });
  }

  /** 脱敏：密码、证件号、令牌永不入审计 */
  private sanitize(input: unknown): unknown {
    if (!input || typeof input !== 'object') return input ?? null;
    const clone: Record<string, unknown> = Array.isArray(input)
      ? { list: input }
      : { ...(input as Record<string, unknown>) };
    for (const key of Object.keys(clone)) {
      if (/password|token|secret|credential/i.test(key)) clone[key] = '***';
    }
    return clone;
  }
}
