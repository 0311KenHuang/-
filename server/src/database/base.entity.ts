import {
  Column,
  CreateDateColumn,
  PrimaryColumn,
  UpdateDateColumn,
} from 'typeorm';

/** MySQL bigint 默认返回字符串，这里统一转 number，避免到处 parseInt */
export const numeric = {
  to: (v?: number | null) => v,
  from: (v?: string | number | null) => (v === null || v === undefined ? v : Number(v)),
};

export abstract class BaseEntity {
  @PrimaryColumn({ type: 'bigint', unsigned: true, generated: 'increment', transformer: numeric })
  id: number;

  @CreateDateColumn({ name: 'created_at', type: 'datetime' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at', type: 'datetime' })
  updatedAt: Date;

  @Column({ name: 'deleted', type: 'tinyint', default: 0, comment: '软删除 0否 1是' })
  deleted: number;
}

/** 带租户的业务表基类：所有查询必须带 tenant_id */
export abstract class TenantEntity extends BaseEntity {
  @Column({ name: 'tenant_id', type: 'bigint', unsigned: true, transformer: numeric })
  tenantId: number;
}
