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
exports.OpsTask = void 0;
const typeorm_1 = require("typeorm");
const base_entity_1 = require("../../database/base.entity");
let OpsTask = class OpsTask extends base_entity_1.TenantEntity {
    taskNo;
    stationId;
    opsType;
    source;
    assigneeId;
    dispatchAt;
    acceptAt;
    arriveAt;
    finishAt;
    deadline;
    status;
    isOverdue;
};
exports.OpsTask = OpsTask;
__decorate([
    (0, typeorm_1.Index)('uk_task_no', { unique: true }),
    (0, typeorm_1.Column)({ name: 'task_no', type: 'varchar', length: 32 }),
    __metadata("design:type", String)
], OpsTask.prototype, "taskNo", void 0);
__decorate([
    (0, typeorm_1.Index)('idx_task_station'),
    (0, typeorm_1.Column)({ name: 'station_id', type: 'bigint', unsigned: true }),
    __metadata("design:type", Number)
], OpsTask.prototype, "stationId", void 0);
__decorate([
    (0, typeorm_1.Column)({
        name: 'ops_type',
        type: 'varchar',
        length: 24,
        comment: '巡检/校准/校验/标液核查/试剂更换/物件更换/故障报告/故障维修',
    }),
    __metadata("design:type", String)
], OpsTask.prototype, "opsType", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 8, default: 'PLAN', comment: 'PLAN/ALARM/MANUAL' }),
    __metadata("design:type", String)
], OpsTask.prototype, "source", void 0);
__decorate([
    (0, typeorm_1.Index)('idx_task_assignee'),
    (0, typeorm_1.Column)({ name: 'assignee_id', type: 'bigint', unsigned: true }),
    __metadata("design:type", Number)
], OpsTask.prototype, "assigneeId", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'dispatch_at', type: 'datetime', nullable: true }),
    __metadata("design:type", Object)
], OpsTask.prototype, "dispatchAt", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'accept_at', type: 'datetime', nullable: true }),
    __metadata("design:type", Object)
], OpsTask.prototype, "acceptAt", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'arrive_at', type: 'datetime', nullable: true }),
    __metadata("design:type", Object)
], OpsTask.prototype, "arriveAt", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'finish_at', type: 'datetime', nullable: true }),
    __metadata("design:type", Object)
], OpsTask.prototype, "finishAt", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'datetime', nullable: true }),
    __metadata("design:type", Object)
], OpsTask.prototype, "deadline", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 16, default: 'DISPATCHED' }),
    __metadata("design:type", String)
], OpsTask.prototype, "status", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'is_overdue', type: 'tinyint', default: 0 }),
    __metadata("design:type", Number)
], OpsTask.prototype, "isOverdue", void 0);
exports.OpsTask = OpsTask = __decorate([
    (0, typeorm_1.Entity)('ops_task')
], OpsTask);
//# sourceMappingURL=ops-task.entity.js.map