import { Column, Entity, Index } from 'typeorm';
import { TenantEntity } from '../../database/base.entity';

/** 角色：SUP 超管 / REG 监管 / ORG 运维管理员 / MGR 运维主管 / ENG 工程师 / ENT 企业 */
@Entity('sys_role')
export class SysRole extends TenantEntity {
  @Index('uk_role_code', ['tenantId', 'roleCode'], { unique: true })
  @Column({ name: 'role_code', type: 'varchar', length: 32 })
  roleCode: string;

  @Column({ type: 'varchar', length: 64 })
  name: string;

  @Column({
    name: 'data_scope',
    type: 'varchar',
    length: 16,
    default: 'ORG',
    comment: 'ALL/REGION/ORG/SELF',
  })
  dataScope: 'ALL' | 'REGION' | 'ORG' | 'SELF';

  @Column({ type: 'varchar', length: 255, nullable: true })
  remark: string | null;
}
