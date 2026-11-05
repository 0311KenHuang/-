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
exports.Hj212Frame = void 0;
const typeorm_1 = require("typeorm");
const base_entity_1 = require("../../database/base.entity");
let Hj212Frame = class Hj212Frame {
    id;
    mn;
    cn;
    qn;
    crcOk;
    source;
    raw;
    params;
    receivedAt;
};
exports.Hj212Frame = Hj212Frame;
__decorate([
    (0, typeorm_1.PrimaryColumn)({ type: 'bigint', unsigned: true, generated: 'increment', transformer: base_entity_1.numeric }),
    __metadata("design:type", Number)
], Hj212Frame.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Index)('idx_frame_mn'),
    (0, typeorm_1.Column)({ type: 'varchar', length: 64 }),
    __metadata("design:type", String)
], Hj212Frame.prototype, "mn", void 0);
__decorate([
    (0, typeorm_1.Index)('idx_frame_cn'),
    (0, typeorm_1.Column)({ type: 'varchar', length: 8 }),
    __metadata("design:type", String)
], Hj212Frame.prototype, "cn", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 32 }),
    __metadata("design:type", String)
], Hj212Frame.prototype, "qn", void 0);
__decorate([
    (0, typeorm_1.Column)({ name: 'crc_ok', type: 'tinyint', default: 1 }),
    __metadata("design:type", Number)
], Hj212Frame.prototype, "crcOk", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 8, default: 'TCP', comment: 'TCP/HTTP' }),
    __metadata("design:type", String)
], Hj212Frame.prototype, "source", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'text', nullable: true }),
    __metadata("design:type", Object)
], Hj212Frame.prototype, "raw", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'json', nullable: true }),
    __metadata("design:type", Object)
], Hj212Frame.prototype, "params", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)({ name: 'received_at', type: 'datetime' }),
    __metadata("design:type", Date)
], Hj212Frame.prototype, "receivedAt", void 0);
exports.Hj212Frame = Hj212Frame = __decorate([
    (0, typeorm_1.Entity)('hj212_frame')
], Hj212Frame);
//# sourceMappingURL=hj212-frame.entity.js.map