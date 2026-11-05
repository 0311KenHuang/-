import { Column, Entity, Index } from 'typeorm';
import { TenantEntity } from '../../database/base.entity';

@Entity('md_enterprise')
export class MdEnterprise extends TenantEntity {
  @Index('uk_credit_code', ['tenantId', 'creditCode'], { unique: true })
  @Column({ name: 'credit_code', type: 'varchar', length: 32, comment: '统一社会信用代码' })
  creditCode: string;

  @Column({ type: 'varchar', length: 160 })
  name: string;

  @Column({ name: 'short_name', type: 'varchar', length: 32, nullable: true, comment: '简称，用于站房编码' })
  shortName: string | null;

  @Column({ name: 'industry_code', type: 'varchar', length: 16, nullable: true })
  industryCode: string | null;

  @Column({ name: 'permit_no', type: 'varchar', length: 64, nullable: true, comment: '排污许可证号' })
  permitNo: string | null;

  @Column({ name: 'region_id', type: 'bigint', unsigned: true })
  regionId: number;

  @Column({ type: 'varchar', length: 255, nullable: true })
  address: string | null;

  @Column({ type: 'decimal', precision: 10, scale: 7, nullable: true })
  lng: string | null;

  @Column({ type: 'decimal', precision: 10, scale: 7, nullable: true })
  lat: string | null;

  @Column({ name: 'is_key_unit', type: 'tinyint', default: 0, comment: '是否重点排污单位' })
  isKeyUnit: number;

  @Column({ type: 'tinyint', default: 1 })
  status: number;
}
