import { Column, Entity, Index } from 'typeorm';
import { TenantEntity } from '../../database/base.entity';

/** 设备：分析仪 / 数采仪 / 流量计。SN 是 HJ212-2025 的设备身份核心 */
@Entity('md_device')
export class MdDevice extends TenantEntity {
  @Index('uk_device_code', ['tenantId', 'deviceCode'], { unique: true })
  @Column({ name: 'device_code', type: 'varchar', length: 32 })
  deviceCode: string;

  @Index('idx_device_station')
  @Column({ name: 'station_id', type: 'bigint', unsigned: true })
  stationId: number;

  @Column({ type: 'varchar', length: 16, comment: 'ANALYZER/DTU/FLOW/OTHER' })
  category: string;

  @Index('uk_device_sn', { unique: true })
  @Column({ type: 'varchar', length: 64, nullable: true, comment: '设备唯一标识（HJ212 强要求）' })
  sn: string | null;

  @Column({ type: 'varchar', length: 64, nullable: true })
  vendor: string | null;

  @Column({ type: 'varchar', length: 64, nullable: true })
  model: string | null;

  @Column({ type: 'varchar', length: 32, nullable: true, comment: '固件版本，判断是否支持 2025 协议' })
  firmware: string | null;

  @Column({ name: 'range_min', type: 'decimal', precision: 18, scale: 6, nullable: true })
  rangeMin: string | null;

  @Column({ name: 'range_max', type: 'decimal', precision: 18, scale: 6, nullable: true })
  rangeMax: string | null;

  @Column({
    name: 'net_status',
    type: 'varchar',
    length: 16,
    default: 'REGISTERED',
    comment: 'REGISTERED/ACTIVATED/CONFIRMED',
  })
  netStatus: string;

  @Column({ type: 'varchar', length: 16, default: 'IN_USE', comment: 'IN_USE/STOPPED/REPLACED/SCRAPPED' })
  status: string;
}
