import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { MdStation } from '../../database/entities/md-station.entity';
import { Ctx } from '../../common/context/request.context';
import { CreateStationDto, QueryStationDto } from './dto/station.dto';

@Injectable()
export class StationService {
  constructor(
    @InjectRepository(MdStation) private readonly repo: Repository<MdStation>,
  ) {}

  /** 分页查询：租户隔离 + 软删除过滤在这里统一处理 */
  async page(query: QueryStationDto) {
    const tenantId = Ctx.tenantId();
    const qb = this.repo
      .createQueryBuilder('s')
      .where('s.tenant_id = :tenantId', { tenantId })
      .andWhere('s.deleted = 0');

    if (query.enterpriseId) qb.andWhere('s.enterprise_id = :eid', { eid: query.enterpriseId });
    if (query.regionId) qb.andWhere('s.region_id = :rid', { rid: query.regionId });
    if (query.stationType) qb.andWhere('s.station_type = :st', { st: query.stationType });
    if (query.bizStatus) qb.andWhere('s.biz_status = :bs', { bs: query.bizStatus });
    if (query.keyword) {
      qb.andWhere('(s.name LIKE :kw OR s.station_code LIKE :kw)', { kw: `%${query.keyword}%` });
    }

    const [list, total] = await qb
      .orderBy('s.id', 'DESC')
      .skip((query.page - 1) * query.pageSize)
      .take(query.pageSize)
      .getManyAndCount();

    return { list, total, page: query.page, pageSize: query.pageSize };
  }

  async detail(id: number) {
    const tenantId = Ctx.tenantId();
    const row = await this.repo.findOne({ where: { id, tenantId, deleted: 0 } });
    if (!row) throw new NotFoundException('站房不存在');
    return row;
  }

  async create(dto: CreateStationDto) {
    const entity = this.repo.create({
      ...dto,
      tenantId: Ctx.tenantId(),
      bizStatus: 'NORMAL',
      lng: dto.lng ?? null,
      lat: dto.lat ?? null,
      address: dto.address ?? null,
      opsOrgId: dto.opsOrgId ?? null,
      opsEngineerId: dto.opsEngineerId ?? null,
    });
    return this.repo.save(entity);
  }

  async update(id: number, dto: Partial<CreateStationDto>) {
    const row = await this.detail(id);
    Object.assign(row, dto);
    return this.repo.save(row);
  }

  /** 站房状态变更必须带原因，监管场景下这是审计要点 */
  async changeStatus(id: number, bizStatus: string, stopReason?: string) {
    const row = await this.detail(id);
    row.bizStatus = bizStatus;
    row.stopReason = bizStatus === 'STOPPED' ? (stopReason ?? null) : null;
    return this.repo.save(row);
  }
}
