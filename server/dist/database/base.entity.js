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
exports.TenantEntity = exports.BaseEntity = exports.numeric = void 0;
const typeorm_1 = require("typeorm");
exports.numeric = {
    to: (v) => v,
    from: (v) => (v === null || v === undefined ? v : Number(v)),
};
class BaseEntity {
    id;
    createdAt;
    updatedAt;
    deleted;
}
exports.BaseEntity = BaseEntity;
__decorate([
    (0, typeorm_1.PrimaryColumn)({ type: 'bigint', unsigned: true, generated: 'increment', transformer: exports.numeric }),
    __metadata("design:type", Number)
], BaseEntity.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)({ name: 'created_at', type: 'datetime' }),
    __metadata("design:type", Date)
], BaseEntity.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)({ name: 'updated_at', type: 'datetime' }),
    __metadata("design:type", Date)
], BaseEntity.prototype, "updatedAt", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'deleted', type: 'tinyint', default: 0, comment: '软删除 0否 1是' }),
    __metadata("design:type", Number)
], BaseEntity.prototype, "deleted", void 0);
class TenantEntity extends BaseEntity {
    tenantId;
}
exports.TenantEntity = TenantEntity;
__decorate([
    (0, typeorm_1.Column)({ name: 'tenant_id', type: 'bigint', unsigned: true, transformer: exports.numeric }),
    __metadata("design:type", Number)
], TenantEntity.prototype, "tenantId", void 0);
//# sourceMappingURL=base.entity.js.map