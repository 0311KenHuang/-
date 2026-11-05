import { Column, Entity, Index } from 'typeorm';
import { TenantEntity } from '../../database/base.entity';

/** 运维任务：SLA 三个时间点独立留痕（接单 / 到场 / 完成） */
@Entity('ops_task')
export class OpsTask extends TenantEntity {
  @Index('uk_task_no', { unique: true })
  @Column({ name: 'task_no', type: 'varchar', length: 32 })
  taskNo: string;

  @Index('idx_task_station')
  @Column({ name: 'station_id', type: 'bigint', unsigned: true })
  stationId: number;

  @Column({
    name: 'ops_type',
    type: 'varchar',
    length: 24,
    comment: '巡检/校准/校验/标液核查/试剂更换/物件更换/故障报告/故障维修',
  })
  opsType: string;

  @Column({ type: 'varchar', length: 8, default: 'PLAN', comment: 'PLAN/ALARM/MANUAL' })
  source: string;

  @Index('idx_task_assignee')
  @Column({ name: 'assignee_id', type: 'bigint', unsigned: true })
  assigneeId: number;

  @Column({ name: 'dispatch_at', type: 'datetime', nullable: true })
  dispatchAt: Date | null;

  @Column({ name: 'accept_at', type: 'datetime', nullable: true })
  acceptAt: Date | null;

  @Column({ name: 'arrive_at', type: 'datetime', nullable: true })
  arriveAt: Date | null;

  @Column({ name: 'finish_at', type: 'datetime', nullable: true })
  finishAt: Date | null;

  @Column({ type: 'datetime', nullable: true })
  deadline: Date | null;

  @Column({ type: 'varchar', length: 16, default: 'DISPATCHED' })
  status: string;

  @Column({ name: 'is_overdue', type: 'tinyint', default: 0 })
  isOverdue: number;
}
