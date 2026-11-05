import { Column, CreateDateColumn, Entity, Index, PrimaryColumn } from 'typeorm';
import { numeric } from '../../database/base.entity';

/**
 * 审计日志：只追加，禁止 UPDATE / DELETE。
 * 生产环境应给应用账号只授 INSERT + SELECT 权限。
 */
@Entity('sys_audit_log')
export class SysAuditLog {
  @PrimaryColumn({ type: 'bigint', unsigned: true, generated: 'increment', transformer: numeric })
  id: number;

  @Column({ name: 'tenant_id', type: 'bigint', unsigned: true })
  tenantId: number;

  @Column({ name: 'user_id', type: 'bigint', unsigned: true })
  userId: number;

  @Column({ name: 'user_name', type: 'varchar', length: 64, nullable: true })
  userName: string | null;

  @Column({ type: 'varchar', length: 64 })
  module: string;

  @Index('idx_audit_action')
  @Column({ type: 'varchar', length: 32, comment: 'CREATE/UPDATE/DELETE/EXPORT/LOGIN/CONTROL' })
  action: string;

  @Column({ name: 'biz_id', type: 'varchar', length: 64, nullable: true })
  bizId: string | null;

  @Column({ name: 'before_json', type: 'json', nullable: true })
  beforeJson: unknown;

  @Column({ name: 'after_json', type: 'json', nullable: true })
  afterJson: unknown;

  @Column({ type: 'varchar', length: 64, nullable: true })
  ip: string | null;

  @Column({ type: 'varchar', length: 255, nullable: true })
  ua: string | null;

  @CreateDateColumn({ name: 'created_at', type: 'datetime' })
  createdAt: Date;
}
