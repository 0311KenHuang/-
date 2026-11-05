import { Column, Entity, Index } from 'typeorm';
import { BaseEntity } from '../../database/base.entity';

/** 租户：REG 监管方 / OPS 运维单位 / ENT 企业 */
@Entity('sys_tenant')
export class SysTenant extends BaseEntity {
  @Index('uk_tenant_code', { unique: true })
  @Column({ name: 'tenant_code', type: 'varchar', length: 32 })
  tenantCode: string;

  @Column({ type: 'varchar', length: 100 })
  name: string;

  @Column({ name: 'tenant_type', type: 'varchar', length: 16, comment: 'REG/OPS/ENT' })
  tenantType: string;

  @Column({ type: 'tinyint', default: 1 })
  status: number;
}
