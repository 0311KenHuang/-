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
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.IngestController = void 0;
const common_1 = require("@nestjs/common");
const config_1 = require("@nestjs/config");
const ingest_service_1 = require("./ingest.service");
const decorators_1 = require("../../common/decorators");
const biz_exception_1 = require("../../common/errors/biz.exception");
let IngestController = class IngestController {
    ingest;
    config;
    constructor(ingest, config) {
        this.ingest = ingest;
        this.config = config;
    }
    async upload(body, token) {
        const expected = this.config.get('INGEST_TOKEN');
        if (expected && token !== expected) {
            throw new biz_exception_1.BizException(biz_exception_1.ErrCode.UNAUTHORIZED, '接入令牌无效', 401);
        }
        const raw = typeof body === 'string' ? body : body?.raw;
        if (!raw)
            throw new biz_exception_1.BizException(biz_exception_1.ErrCode.BAD_REQUEST, '报文为空');
        const ack = await this.ingest.handleRaw(raw, 'HTTP');
        return { ack };
    }
    stat() {
        return this.ingest.stat;
    }
};
exports.IngestController = IngestController;
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('hj212'),
    __param(0, (0, common_1.Body)()),
    __param(1, (0, common_1.Headers)('x-ingest-token')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String]),
    __metadata("design:returntype", Promise)
], IngestController.prototype, "upload", null);
__decorate([
    (0, common_1.Get)('stat'),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", void 0)
], IngestController.prototype, "stat", null);
exports.IngestController = IngestController = __decorate([
    (0, common_1.Controller)('ingest'),
    __metadata("design:paramtypes", [ingest_service_1.IngestService,
        config_1.ConfigService])
], IngestController);
//# sourceMappingURL=ingest.controller.js.map