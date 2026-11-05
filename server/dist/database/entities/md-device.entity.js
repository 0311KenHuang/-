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
exports.MdDevice = void 0;
const typeorm_1 = require("typeorm");
const base_entity_1 = require("../../database/base.entity");
let MdDevice = class MdDevice extends base_entity_1.TenantEntity {
    deviceCode;
    stationId;
    category;
    sn;
    vendor;
    model;
    firmware;
    rangeMin;
    rangeMax;
    netStatus;
    status;
};
exports.MdDevice = MdDevice;
__decorate([
    (0, typeorm_1.Index)('uk_device_code', ['tenantId', 'deviceCode'], { unique: true }),
    (0, typeorm_1.Column)({ name: 'device_code', type: 'varchar', length: 32 }),
    __metadata("design:type", String)
], MdDevice.prototype, "deviceCode", void 0);
__decorate([
    (0, typeorm_1.Index)('idx_device_station'),
    (0, typeorm_1.Column)({ name: 'station_id', type: 'bigint', unsigned: true }),
    __metadata("design:type", Number)
], MdDevice.prototype, "stationId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 16, comment: 'ANALYZER/DTU/FLOW/OTHER' }),
    __metadata("design:type", String)
], MdDevice.prototype, "category", void 0);
__decorate([
    (0, typeorm_1.Index)('uk_device_sn', { unique: true }),
    (0, typeorm_1.Column)({ type: 'varchar', length: 64, nullable: true, comment: '设备唯一标识（HJ212 强要求）' }),
    __metadata("design:type", Object)
], MdDevice.prototype, "sn", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 64, nullable: true }),
    __metadata("design:type", Object)
], MdDevice.prototype, "vendor", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 64, nullable: true }),
    __metadata("design:type", Object)
], MdDevice.prototype, "model", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 32, nullable: true, comment: '固件版本，判断是否支持 2025 协议' }),
    __metadata("design:type", Object)
], MdDevice.prototype, "firmware", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'range_min', type: 'decimal', precision: 18, scale: 6, nullable: true }),
    __metadata("design:type", Object)
], MdDevice.prototype, "rangeMin", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'range_max', type: 'decimal', precision: 18, scale: 6, nullable: true }),
    __metadata("design:type", Object)
], MdDevice.prototype, "rangeMax", void 0);
__decorate([
    (0, typeorm_1.Column)({
        name: 'net_status',
        type: 'varchar',
        length: 16,
        default: 'REGISTERED',
        comment: 'REGISTERED/ACTIVATED/CONFIRMED',
    }),
    __metadata("design:type", String)
], MdDevice.prototype, "netStatus", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 16, default: 'IN_USE', comment: 'IN_USE/STOPPED/REPLACED/SCRAPPED' }),
    __metadata("design:type", String)
], MdDevice.prototype, "status", void 0);
exports.MdDevice = MdDevice = __decorate([
    (0, typeorm_1.Entity)('md_device')
], MdDevice);
//# sourceMappingURL=md-device.entity.js.map