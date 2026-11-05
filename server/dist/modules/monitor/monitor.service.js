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
exports.MonitorService = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const md_station_entity_1 = require("../../database/entities/md-station.entity");
const alm_event_entity_1 = require("../../database/entities/alm-event.entity");
const hj212_frame_entity_1 = require("../../database/entities/hj212-frame.entity");
const request_context_1 = require("../../common/context/request.context");
let MonitorService = class MonitorService {
    stations;
    alarms;
    frames;
    constructor(stations, alarms, frames) {
        this.stations = stations;
        this.alarms = alarms;
        this.frames = frames;
    }
    async overview() {
        const tenantId = request_context_1.Ctx.tenantId();
        const statusRows = await this.stations
            .createQueryBuilder('s')
            .select('s.biz_status', 'status')
            .addSelect('COUNT(1)', 'cnt')
            .where('s.tenant_id = :tenantId AND s.deleted = 0', { tenantId })
            .groupBy('s.biz_status')
            .getRawMany();
        const byStatus = {};
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
};
exports.MonitorService = MonitorService;
exports.MonitorService = MonitorService = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(md_station_entity_1.MdStation)),
    __param(1, (0, typeorm_1.InjectRepository)(alm_event_entity_1.AlmEvent)),
    __param(2, (0, typeorm_1.InjectRepository)(hj212_frame_entity_1.Hj212Frame)),
    __metadata("design:paramtypes", [typeorm_2.Repository,
        typeorm_2.Repository,
        typeorm_2.Repository])
], MonitorService);
//# sourceMappingURL=monitor.service.js.map