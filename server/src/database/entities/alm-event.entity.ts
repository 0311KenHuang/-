import { Column, Entity, Index } from 'typeorm';
import { TenantEntity } from '../../database/base.entity';

/** 报警事件：OPEN → ACK → PROCESSING → CLOSED / FALSE_ALARM */
@Entity('alm_event')
export class AlmEvent extends TenantEntity {
  @Index('uk_event_no', { unique: true })
  @Column({ name: 'event_no', type: 'varchar', length: 32 })
  eventNo: string;

  @Column({ name: 'rule_id', type: 'bigint', unsigned: true })
  ruleId: number;

  @Index('idx_event_station')
  @Column({ name: 'station_id', type: 'bigint', unsigned: true })
  stationId: number;

  @Column({ name: 'factor_code', type: 'varchar', length: 16, nullable: true })
  factorCode: string | null;

  @Column({ type: 'varchar', length: 4, comment: 'P1/P2/P3' })
  level: string;

  @Column({ type: 'decimal', precision: 18, scale: 6, nullable: true })
  value: string | null;

  @Column({ type: 'decimal', precision: 18, scale: 6, nullable: true })
  threshold: string | null;

  @Column({ name: 'begin_at', type: 'datetime' })
  beginAt: Date;

  @Column({ name: 'end_at', type: 'datetime', nullable: true })
  endAt: Date | null;

  @Column({ type: 'varchar', length: 16, default: 'OPEN', comment: 'OPEN/ACK/PROCESSING/CLOSED/FALSE_ALARM' })
  status: string;

  @Column({ name: 'owner_id', type: 'bigint', unsigned: true, nullable: true })
  ownerId: number | null;

  @Column({ name: 'related_task_id', type: 'bigint', unsigned: true, nullable: true })
  relatedTaskId: number | null;
}
