"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.StationService = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const md_station_entity_1 = require("../../database/entities/md-station.entity");
const request_context_1 = require("../../common/context/request.context");
let StationService = class StationService {
    repo;
    constructor(repo) {
        this.repo = repo;
    }
    async page(query) {
        const tenantId = request_context_1.Ctx.tenantId();
        const qb = this.repo
            .createQueryBuilder('s')
            .where('s.tenant_id = :tenantId', { tenantId })
            .andWhere('s.deleted = 0');
        if (query.enterpriseId)
            qb.andWhere('s.enterprise_id = :eid', { eid: query.enterpriseId });
        if (query.regionId)
            qb.andWhere('s.region_id = :rid', { rid: query.regionId });
        if (query.stationType)
            qb.andWhere('s.station_type = :st', { st: query.stationType });
        if (query.bizStatus)
            qb.andWhere('s.biz_status = :bs', { bs: query.bizStatus });
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
    async detail(id) {
        const tenantId = request_context_1.Ctx.tenantId();
        const row = await this.repo.findOne({ where: { id, tenantId, deleted: 0 } });
        if (!row)
            throw new common_1.NotFoundException('站房不存在');
        return row;
    }
    async create(dto) {
        const entity = this.repo.create({
            ...dto,
            tenantId: request_context_1.Ctx.tenantId(),
            bizStatus: 'NORMAL',
            lng: dto.lng ?? null,
            lat: dto.lat ?? null,
            address: dto.address ?? null,
            opsOrgId: dto.opsOrgId ?? null,
            opsEngineerId: dto.opsEngineerId ?? null,
        });
        return this.repo.save(entity);
    }
    async update(id, dto) {
        const row = await this.detail(id);
        Object.assign(row, dto);
        return this.repo.save(row);
    }
    async changeStatus(id, bizStatus, stopReason) {
        const row = await this.detail(id);
        row.bizStatus = bizStatus;
        row.stopReason = bizStatus === 'STOPPED' ? (stopReason ?? null) : null;
        return this.repo.save(row);
    }
};
exports.StationService = StationService;
exports.StationService = StationService = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(md_station_entity_1.MdStation)),
    __metadata("design:paramtypes", [typeorm_2.Repository])
], StationService);
//# sourceMappingURL=station.service.js.map