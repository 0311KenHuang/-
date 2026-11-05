import { Column, Entity, Index } from 'typeorm';
import { TenantEntity } from '../../database/base.entity';

/** 站房：最小运维管理单元（一个站房可对应多个排口） */
@Entity('md_station')
export class MdStation extends TenantEntity {
  @Index('uk_station_code', ['tenantId', 'stationCode'], { unique: true })
  @Column({ name: 'station_code', type: 'varchar', length: 32, comment: '可读业务编码，如 FHZY-S-01' })
  stationCode: string;

  @Column({ type: 'varchar', length: 100, comment: '命名规则：企业简称-介质' })
  name: string;

  @Index('idx_station_type')
  @Column({
    name: 'station_type',
    type: 'varchar',
    length: 20,
    comment: 'WASTE_WATER/WASTE_GAS/VOC/INCINERATION/AIR/GROUNDWATER',
  })
  stationType: string;

  @Index('idx_station_ent')
  @Column({ name: 'enterprise_id', type: 'bigint', unsigned: true })
  enterpriseId: number;

  @Column({ name: 'region_id', type: 'bigint', unsigned: true })
  regionId: number;

  @Column({ type: 'decimal', precision: 10, scale: 7, nullable: true })
  lng: string | null;

  @Column({ type: 'decimal', precision: 10, scale: 7, nullable: true })
  lat: string | null;

  @Column({ type: 'varchar', length: 255, nullable: true })
  address: string | null;

  @Column({
    name: 'biz_status',
    type: 'varchar',
    length: 16,
    default: 'NORMAL',
    comment: 'NORMAL/ABNORMAL/STOPPED/MAINTAINING',
  })
  bizStatus: string;

  @Column({ name: 'stop_reason', type: 'varchar', length: 255, nullable: true })
  stopReason: string | null;

  @Index('idx_station_org')
  @Column({ name: 'ops_org_id', type: 'bigint', unsigned: true, nullable: true, comment: '责任运维单位' })
  opsOrgId: number | null;

  @Column({ name: 'ops_engineer_id', type: 'bigint', unsigned: true, nullable: true })
  opsEngineerId: number | null;

  @Column({ name: 'is_focus', type: 'tinyint', default: 0, comment: '重点关注' })
  isFocus: number;

  @Column({ name: 'qr_token', type: 'varchar', length: 64, nullable: true })
  qrToken: string | null;
}
