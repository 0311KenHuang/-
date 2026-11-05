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
exports.SysUser = void 0;
const typeorm_1 = require("typeorm");
const base_entity_1 = require("../../database/base.entity");
const sys_role_entity_1 = require("./sys-role.entity");
let SysUser = class SysUser extends base_entity_1.TenantEntity {
    username;
    passwordHash;
    realName;
    orgId;
    phone;
    certNo;
    status;
    lastLoginAt;
    roles;
};
exports.SysUser = SysUser;
__decorate([
    (0, typeorm_1.Index)('uk_username', ['tenantId', 'username'], { unique: true }),
    (0, typeorm_1.Column)({ type: 'varchar', length: 64 }),
    __metadata("design:type", String)
], SysUser.prototype, "username", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'password_hash', type: 'varchar', length: 100 }),
    __metadata("design:type", String)
], SysUser.prototype, "passwordHash", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'real_name', type: 'varchar', length: 64 }),
    __metadata("design:type", String)
], SysUser.prototype, "realName", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'org_id', type: 'bigint', unsigned: true, nullable: true }),
    __metadata("design:type", Object)
], SysUser.prototype, "orgId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 32, nullable: true }),
    __metadata("design:type", Object)
], SysUser.prototype, "phone", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'cert_no', type: 'varchar', length: 64, nullable: true, comment: '运维工资质证书号' }),
    __metadata("design:type", Object)
], SysUser.prototype, "certNo", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'tinyint', default: 1, comment: '1正常 0停用 2锁定' }),
    __metadata("design:type", Number)
], SysUser.prototype, "status", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'last_login_at', type: 'datetime', nullable: true }),
    __metadata("design:type", Object)
], SysUser.prototype, "lastLoginAt", void 0);
__decorate([
    (0, typeorm_1.ManyToMany)(() => sys_role_entity_1.SysRole, { eager: true }),
    (0, typeorm_1.JoinTable)({
        name: 'sys_user_role',
        joinColumn: { name: 'user_id', referencedColumnName: 'id' },
        inverseJoinColumn: { name: 'role_id', referencedColumnName: 'id' },
    }),
    __metadata("design:type", Array)
], SysUser.prototype, "roles", void 0);
exports.SysUser = SysUser = __decorate([
    (0, typeorm_1.Entity)('sys_user')
], SysUser);
//# sourceMappingURL=sys-user.entity.js.map