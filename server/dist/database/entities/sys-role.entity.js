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
exports.SysRole = void 0;
const typeorm_1 = require("typeorm");
const base_entity_1 = require("../../database/base.entity");
let SysRole = class SysRole extends base_entity_1.TenantEntity {
    roleCode;
    name;
    dataScope;
    remark;
};
exports.SysRole = SysRole;
__decorate([
    (0, typeorm_1.Index)('uk_role_code', ['tenantId', 'roleCode'], { unique: true }),
    (0, typeorm_1.Column)({ name: 'role_code', type: 'varchar', length: 32 }),
    __metadata("design:type", String)
], SysRole.prototype, "roleCode", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 64 }),
    __metadata("design:type", String)
], SysRole.prototype, "name", void 0);
__decorate([
    (0, typeorm_1.Column)({
        name: 'data_scope',
        type: 'varchar',
        length: 16,
        default: 'ORG',
        comment: 'ALL/REGION/ORG/SELF',
    }),
    __metadata("design:type", String)
], SysRole.prototype, "dataScope", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255, nullable: true }),
    __metadata("design:type", Object)
], SysRole.prototype, "remark", void 0);
exports.SysRole = SysRole = __decorate([
    (0, typeorm_1.Entity)('sys_role')
], SysRole);
//# sourceMappingURL=sys-role.entity.js.map