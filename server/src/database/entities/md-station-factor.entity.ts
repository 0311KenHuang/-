import { Column, Entity, Index } from 'typeorm';
import { TenantEntity } from '../../database/base.entity';

/** 站房因子配置：决定一个站房上报哪些因子、量程与限值 */
@Entity('md_station_factor')
export class MdStationFactor extends TenantEntity {
  @Index('uk_station_factor', ['stationId', 'factorCode'], { unique: true })
  @Column({ name: 'station_id', type: 'bigint', unsigned: true })
  stationId: number;

  @Column({ name: 'factor_code', type: 'varchar', length: 16, comment: 'HJ212 参数编码，如 a21026' })
  factorCode: string;

  @Column({ name: 'device_id', type: 'bigint', unsigned: true, nullable: true })
  deviceId: number | null;

  @Column({ type: 'varchar', length: 16, nullable: true })
  unit: string | null;

  @Column({ name: 'range_min', type: 'decimal', precision: 18, scale: 6, nullable: true })
  rangeMin: string | null;

  @Column({ name: 'range_max', type: 'decimal', precision: 18, scale: 6, nullable: true })
  rangeMax: string | null;

  @Column({ name: 'std_limit', type: 'decimal', precision: 18, scale: 6, nullable: true, comment: '执行限值' })
  stdLimit: string | null;

  @Column({ name: 'is_main', type: 'tinyint', default: 0, comment: '是否主因子（卡片展示）' })
  isMain: number;

  @Column({ type: 'int', default: 0 })
  sort: number;
}
