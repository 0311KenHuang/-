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
exports.MdEnterprise = void 0;
const typeorm_1 = require("typeorm");
const base_entity_1 = require("../../database/base.entity");
let MdEnterprise = class MdEnterprise extends base_entity_1.TenantEntity {
    creditCode;
    name;
    shortName;
    industryCode;
    permitNo;
    regionId;
    address;
    lng;
    lat;
    isKeyUnit;
    status;
};
exports.MdEnterprise = MdEnterprise;
__decorate([
    (0, typeorm_1.Index)('uk_credit_code', ['tenantId', 'creditCode'], { unique: true }),
    (0, typeorm_1.Column)({ name: 'credit_code', type: 'varchar', length: 32, comment: '统一社会信用代码' }),
    __metadata("design:type", String)
], MdEnterprise.prototype, "creditCode", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 160 }),
    __metadata("design:type", String)
], MdEnterprise.prototype, "name", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'short_name', type: 'varchar', length: 32, nullable: true, comment: '简称，用于站房编码' }),
    __metadata("design:type", Object)
], MdEnterprise.prototype, "shortName", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'industry_code', type: 'varchar', length: 16, nullable: true }),
    __metadata("design:type", Object)
], MdEnterprise.prototype, "industryCode", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'permit_no', type: 'varchar', length: 64, nullable: true, comment: '排污许可证号' }),
    __metadata("design:type", Object)
], MdEnterprise.prototype, "permitNo", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'region_id', type: 'bigint', unsigned: true }),
    __metadata("design:type", Number)
], MdEnterprise.prototype, "regionId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255, nullable: true }),
    __metadata("design:type", Object)
], MdEnterprise.prototype, "address", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'decimal', precision: 10, scale: 7, nullable: true }),
    __metadata("design:type", Object)
], MdEnterprise.prototype, "lng", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'decimal', precision: 10, scale: 7, nullable: true }),
    __metadata("design:type", Object)
], MdEnterprise.prototype, "lat", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'is_key_unit', type: 'tinyint', default: 0, comment: '是否重点排污单位' }),
    __metadata("design:type", Number)
], MdEnterprise.prototype, "isKeyUnit", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'tinyint', default: 1 }),
    __metadata("design:type", Number)
], MdEnterprise.prototype, "status", void 0);
exports.MdEnterprise = MdEnterprise = __decorate([
    (0, typeorm_1.Entity)('md_enterprise')
], MdEnterprise);
//# sourceMappingURL=md-enterprise.entity.js.map