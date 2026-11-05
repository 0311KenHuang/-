import { Column, Entity, Index, JoinTable, ManyToMany } from 'typeorm';
import { TenantEntity } from '../../database/base.entity';
import { SysRole } from './sys-role.entity';

@Entity('sys_user')
export class SysUser extends TenantEntity {
  @Index('uk_username', ['tenantId', 'username'], { unique: true })
  @Column({ type: 'varchar', length: 64 })
  username: string;

  @Column({ name: 'password_hash', type: 'varchar', length: 100 })
  passwordHash: string;

  @Column({ name: 'real_name', type: 'varchar', length: 64 })
  realName: string;

  @Column({ name: 'org_id', type: 'bigint', unsigned: true, nullable: true })
  orgId: number | null;

  @Column({ type: 'varchar', length: 32, nullable: true })
  phone: string | null;

  @Column({ name: 'cert_no', type: 'varchar', length: 64, nullable: true, comment: '运维工资质证书号' })
  certNo: string | null;

  @Column({ type: 'tinyint', default: 1, comment: '1正常 0停用 2锁定' })
  status: number;

  @Column({ name: 'last_login_at', type: 'datetime', nullable: true })
  lastLoginAt: Date | null;

  @ManyToMany(() => SysRole, { eager: true })
  @JoinTable({
    name: 'sys_user_role',
    joinColumn: { name: 'user_id', referencedColumnName: 'id' },
    inverseJoinColumn: { name: 'role_id', referencedColumnName: 'id' },
  })
  roles: SysRole[];
}
