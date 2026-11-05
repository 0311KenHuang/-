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
exports.SysTenant = void 0;
const typeorm_1 = require("typeorm");
const base_entity_1 = require("../../database/base.entity");
let SysTenant = class SysTenant extends base_entity_1.BaseEntity {
    tenantCode;
    name;
    tenantType;
    status;
};
exports.SysTenant = SysTenant;
__decorate([
    (0, typeorm_1.Index)('uk_tenant_code', { unique: true }),
    (0, typeorm_1.Column)({ name: 'tenant_code', type: 'varchar', length: 32 }),
    __metadata("design:type", String)
], SysTenant.prototype, "tenantCode", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 100 }),
    __metadata("design:type", String)
], SysTenant.prototype, "name", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'tenant_type', type: 'varchar', length: 16, comment: 'REG/OPS/ENT' }),
    __metadata("design:type", String)
], SysTenant.prototype, "tenantType", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'tinyint', default: 1 }),
    __metadata("design:type", Number)
], SysTenant.prototype, "status", void 0);
exports.SysTenant = SysTenant = __decorate([
    (0, typeorm_1.Entity)('sys_tenant')
], SysTenant);
//# sourceMappingURL=sys-tenant.entity.js.map