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
Object.defineProperty(exports, "__esModule", { value: true });
exports.MdStation = void 0;
const typeorm_1 = require("typeorm");
const base_entity_1 = require("../../database/base.entity");
let MdStation = class MdStation extends base_entity_1.TenantEntity {
    stationCode;
    name;
    stationType;
    enterpriseId;
    regionId;
    lng;
    lat;
    address;
    bizStatus;
    stopReason;
    opsOrgId;
    opsEngineerId;
    isFocus;
    qrToken;
};
exports.MdStation = MdStation;
__decorate([
    (0, typeorm_1.Index)('uk_station_code', ['tenantId', 'stationCode'], { unique: true }),
    (0, typeorm_1.Column)({ name: 'station_code', type: 'varchar', length: 32, comment: '可读业务编码，如 FHZY-S-01' }),
    __metadata("design:type", String)
], MdStation.prototype, "stationCode", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 100, comment: '命名规则：企业简称-介质' }),
    __metadata("design:type", String)
], MdStation.prototype, "name", void 0);
__decorate([
    (0, typeorm_1.Index)('idx_station_type'),
    (0, typeorm_1.Column)({
        name: 'station_type',
        type: 'varchar',
        length: 20,
        comment: 'WASTE_WATER/WASTE_GAS/VOC/INCINERATION/AIR/GROUNDWATER',
    }),
    __metadata("design:type", String)
], MdStation.prototype, "stationType", void 0);
__decorate([
    (0, typeorm_1.Index)('idx_station_ent'),
    (0, typeorm_1.Column)({ name: 'enterprise_id', type: 'bigint', unsigned: true }),
    __metadata("design:type", Number)
], MdStation.prototype, "enterpriseId", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'region_id', type: 'bigint', unsigned: true }),
    __metadata("design:type", Number)
], MdStation.prototype, "regionId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'decimal', precision: 10, scale: 7, nullable: true }),
    __metadata("design:type", Object)
], MdStation.prototype, "lng", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'decimal', precision: 10, scale: 7, nullable: true }),
    __metadata("design:type", Object)
], MdStation.prototype, "lat", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255, nullable: true }),
    __metadata("design:type", Object)
], MdStation.prototype, "address", void 0);
__decorate([
    (0, typeorm_1.Column)({
        name: 'biz_status',
        type: 'varchar',
        length: 16,
        default: 'NORMAL',
        comment: 'NORMAL/ABNORMAL/STOPPED/MAINTAINING',
    }),
    __metadata("design:type", String)
], MdStation.prototype, "bizStatus", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'stop_reason', type: 'varchar', length: 255, nullable: true }),
    __metadata("design:type", Object)
], MdStation.prototype, "stopReason", void 0);
__decorate([
    (0, typeorm_1.Index)('idx_station_org'),
    (0, typeorm_1.Column)({ name: 'ops_org_id', type: 'bigint', unsigned: true, nullable: true, comment: '责任运维单位' }),
    __metadata("design:type", Object)
], MdStation.prototype, "opsOrgId", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'ops_engineer_id', type: 'bigint', unsigned: true, nullable: true }),
    __metadata("design:type", Object)
], MdStation.prototype, "opsEngineerId", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'is_focus', type: 'tinyint', default: 0, comment: '重点关注' }),
    __metadata("design:type", Number)
], MdStation.prototype, "isFocus", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'qr_token', type: 'varchar', length: 64, nullable: true }),
    __metadata("design:type", Object)
], MdStation.prototype, "qrToken", void 0);
exports.MdStation = MdStation = __decorate([
    (0, typeorm_1.Entity)('md_station')
], MdStation);
//# sourceMappingURL=md-station.entity.js.map