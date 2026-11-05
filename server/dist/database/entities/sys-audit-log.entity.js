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
exports.SysAuditLog = void 0;
const typeorm_1 = require("typeorm");
const base_entity_1 = require("../../database/base.entity");
let SysAuditLog = class SysAuditLog {
    id;
    tenantId;
    userId;
    userName;
    module;
    action;
    bizId;
    beforeJson;
    afterJson;
    ip;
    ua;
    createdAt;
};
exports.SysAuditLog = SysAuditLog;
__decorate([
    (0, typeorm_1.PrimaryColumn)({ type: 'bigint', unsigned: true, generated: 'increment', transformer: base_entity_1.numeric }),
    __metadata("design:type", Number)
], SysAuditLog.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'tenant_id', type: 'bigint', unsigned: true }),
    __metadata("design:type", Number)
], SysAuditLog.prototype, "tenantId", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'user_id', type: 'bigint', unsigned: true }),
    __metadata("design:type", Number)
], SysAuditLog.prototype, "userId", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'user_name', type: 'varchar', length: 64, nullable: true }),
    __metadata("design:type", Object)
], SysAuditLog.prototype, "userName", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 64 }),
    __metadata("design:type", String)
], SysAuditLog.prototype, "module", void 0);
__decorate([
    (0, typeorm_1.Index)('idx_audit_action'),
    (0, typeorm_1.Column)({ type: 'varchar', length: 32, comment: 'CREATE/UPDATE/DELETE/EXPORT/LOGIN/CONTROL' }),
    __metadata("design:type", String)
], SysAuditLog.prototype, "action", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'biz_id', type: 'varchar', length: 64, nullable: true }),
    __metadata("design:type", Object)
], SysAuditLog.prototype, "bizId", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'before_json', type: 'json', nullable: true }),
    __metadata("design:type", Object)
], SysAuditLog.prototype, "beforeJson", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'after_json', type: 'json', nullable: true }),
    __metadata("design:type", Object)
], SysAuditLog.prototype, "afterJson", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 64, nullable: true }),
    __metadata("design:type", Object)
], SysAuditLog.prototype, "ip", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255, nullable: true }),
    __metadata("design:type", Object)
], SysAuditLog.prototype, "ua", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)({ name: 'created_at', type: 'datetime' }),
    __metadata("design:type", Date)
], SysAuditLog.prototype, "createdAt", void 0);
exports.SysAuditLog = SysAuditLog = __decorate([
    (0, typeorm_1.Entity)('sys_audit_log')
], SysAuditLog);
//# sourceMappingURL=sys-audit-log.entity.js.map