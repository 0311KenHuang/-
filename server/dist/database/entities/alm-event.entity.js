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
exports.AlmEvent = void 0;
const typeorm_1 = require("typeorm");
const base_entity_1 = require("../../database/base.entity");
let AlmEvent = class AlmEvent extends base_entity_1.TenantEntity {
    eventNo;
    ruleId;
    stationId;
    factorCode;
    level;
    value;
    threshold;
    beginAt;
    endAt;
    status;
    ownerId;
    relatedTaskId;
};
exports.AlmEvent = AlmEvent;
__decorate([
    (0, typeorm_1.Index)('uk_event_no', { unique: true }),
    (0, typeorm_1.Column)({ name: 'event_no', type: 'varchar', length: 32 }),
    __metadata("design:type", String)
], AlmEvent.prototype, "eventNo", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'rule_id', type: 'bigint', unsigned: true }),
    __metadata("design:type", Number)
], AlmEvent.prototype, "ruleId", void 0);
__decorate([
    (0, typeorm_1.Index)('idx_event_station'),
    (0, typeorm_1.Column)({ name: 'station_id', type: 'bigint', unsigned: true }),
    __metadata("design:type", Number)
], AlmEvent.prototype, "stationId", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'factor_code', type: 'varchar', length: 16, nullable: true }),
    __metadata("design:type", Object)
], AlmEvent.prototype, "factorCode", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 4, comment: 'P1/P2/P3' }),
    __metadata("design:type", String)
], AlmEvent.prototype, "level", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'decimal', precision: 18, scale: 6, nullable: true }),
    __metadata("design:type", Object)
], AlmEvent.prototype, "value", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'decimal', precision: 18, scale: 6, nullable: true }),
    __metadata("design:type", Object)
], AlmEvent.prototype, "threshold", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'begin_at', type: 'datetime' }),
    __metadata("design:type", Date)
], AlmEvent.prototype, "beginAt", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'end_at', type: 'datetime', nullable: true }),
    __metadata("design:type", Object)
], AlmEvent.prototype, "endAt", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 16, default: 'OPEN', comment: 'OPEN/ACK/PROCESSING/CLOSED/FALSE_ALARM' }),
    __metadata("design:type", String)
], AlmEvent.prototype, "status", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'owner_id', type: 'bigint', unsigned: true, nullable: true }),
    __metadata("design:type", Object)
], AlmEvent.prototype, "ownerId", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'related_task_id', type: 'bigint', unsigned: true, nullable: true }),
    __metadata("design:type", Object)
], AlmEvent.prototype, "relatedTaskId", void 0);
exports.AlmEvent = AlmEvent = __decorate([
    (0, typeorm_1.Entity)('alm_event')
], AlmEvent);
//# sourceMappingURL=alm-event.entity.js.map