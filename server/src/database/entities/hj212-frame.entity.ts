import { Column, CreateDateColumn, Entity, Index, PrimaryColumn } from 'typeorm';
import { numeric } from '../../database/base.entity';

/**
 * 原始报文落库：争议时唯一证据。
 * 生产环境建议保留 7~30 天，之后转对象存储。
 */
@Entity('hj212_frame')
export class Hj212Frame {
  @PrimaryColumn({ type: 'bigint', unsigned: true, generated: 'increment', transformer: numeric })
  id: number;

  @Index('idx_frame_mn')
  @Column({ type: 'varchar', length: 64 })
  mn: string;

  @Index('idx_frame_cn')
  @Column({ type: 'varchar', length: 8 })
  cn: string;

  @Column({ type: 'varchar', length: 32 })
  qn: string;

  @Column({ name: 'crc_ok', type: 'tinyint', default: 1 })
  crcOk: number;

  @Column({ type: 'varchar', length: 8, default: 'TCP', comment: 'TCP/HTTP' })
  source: string;

  @Column({ type: 'text', nullable: true })
  raw: string | null;

  @Column({ type: 'json', nullable: true })
  params: Record<string, string> | null;

  @CreateDateColumn({ name: 'received_at', type: 'datetime' })
  receivedAt: Date;
}
