import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { MdStation } from '../../database/entities/md-station.entity';
import { AlmEvent } from '../../database/entities/alm-event.entity';
import { Hj212Frame } from '../../database/entities/hj212-frame.entity';
import { Ctx } from '../../common/context/request.context';

@Injectable()
export class MonitorService {
  constructor(
    @InjectRepository(MdStation) private readonly stations: Repository<MdStation>,
    @InjectRepository(AlmEvent) private readonly alarms: Repository<AlmEvent>,
    @InjectRepository(Hj212Frame) private readonly frames: Repository<Hj212Frame>,
  ) {}

  /** 工作台顶部指标：一次查三张表，够用且不引入额外聚合表 */
  async overview() {
    const tenantId = Ctx.tenantId();

    const statusRows = await this.stations
      .createQueryBuilder('s')
      .select('s.biz_status', 'status')
      .addSelect('COUNT(1)', 'cnt')
      .where('s.tenant_id = :tenantId AND s.deleted = 0', { tenantId })
      .groupBy('s.biz_status')
      .getRawMany<{ status: string; cnt: string }>();

    const byStatus: Record<string, number> = {};
    let stationTotal = 0;
    for (const r of statusRows) {
      byStatus[r.status] = Number(r.cnt);
      stationTotal += Number(r.cnt);
    }

    const [alarmOpen, frameToday, lastFrame] = await Promise.all([
      this.alarms.count({
        where: { tenantId, deleted: 0 },
      }),
      this.frames
        .createQueryBuilder('f')
        .where('f.received_at >= CURDATE()')
        .getCount(),
      this.frames.findOne({ where: {}, order: { receivedAt: 'DESC' } }),
    ]);

    return {
      stationTotal,
      stationByStatus: byStatus,
      alarmTotal: alarmOpen,
      frameToday,
      lastFrameAt: lastFrame?.receivedAt ?? null,
    };
  }
}
