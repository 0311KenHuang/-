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
exports.MdStationFactor = void 0;
const typeorm_1 = require("typeorm");
const base_entity_1 = require("../../database/base.entity");
let MdStationFactor = class MdStationFactor extends base_entity_1.TenantEntity {
    stationId;
    factorCode;
    deviceId;
    unit;
    rangeMin;
    rangeMax;
    stdLimit;
    isMain;
    sort;
};
exports.MdStationFactor = MdStationFactor;
__decorate([
    (0, typeorm_1.Index)('uk_station_factor', ['stationId', 'factorCode'], { unique: true }),
    (0, typeorm_1.Column)({ name: 'station_id', type: 'bigint', unsigned: true }),
    __metadata("design:type", Number)
], MdStationFactor.prototype, "stationId", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'factor_code', type: 'varchar', length: 16, comment: 'HJ212 参数编码，如 a21026' }),
    __metadata("design:type", String)
], MdStationFactor.prototype, "factorCode", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'device_id', type: 'bigint', unsigned: true, nullable: true }),
    __metadata("design:type", Object)
], MdStationFactor.prototype, "deviceId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 16, nullable: true }),
    __metadata("design:type", Object)
], MdStationFactor.prototype, "unit", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'range_min', type: 'decimal', precision: 18, scale: 6, nullable: true }),
    __metadata("design:type", Object)
], MdStationFactor.prototype, "rangeMin", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'range_max', type: 'decimal', precision: 18, scale: 6, nullable: true }),
    __metadata("design:type", Object)
], MdStationFactor.prototype, "rangeMax", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'std_limit', type: 'decimal', precision: 18, scale: 6, nullable: true, comment: '执行限值' }),
    __metadata("design:type", Object)
], MdStationFactor.prototype, "stdLimit", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'is_main', type: 'tinyint', default: 0, comment: '是否主因子（卡片展示）' }),
    __metadata("design:type", Number)
], MdStationFactor.prototype, "isMain", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], MdStationFactor.prototype, "sort", void 0);
exports.MdStationFactor = MdStationFactor = __decorate([
    (0, typeorm_1.Entity)('md_station_factor')
], MdStationFactor);
//# sourceMappingURL=md-station-factor.entity.js.map