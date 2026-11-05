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
var IngestService_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.IngestService = void 0;
const common_1 = require("@nestjs/common");
const config_1 = require("@nestjs/config");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const hj212_frame_entity_1 = require("../../database/entities/hj212-frame.entity");
const codec_1 = require("./hj212/codec");
let IngestService = IngestService_1 = class IngestService {
    frames;
    config;
    logger = new common_1.Logger(IngestService_1.name);
    stat = { received: 0, accepted: 0, crcFailed: 0, parseFailed: 0 };
    constructor(frames, config) {
        this.frames = frames;
        this.config = config;
    }
    async handleRaw(raw, source) {
        this.stat.received++;
        const frame = codec_1.Hj212Codec.decode(raw);
        if (!frame) {
            this.stat.parseFailed++;
            return null;
        }
        if (!frame.crcOk) {
            this.stat.crcFailed++;
            this.logger.warn(`CRC 错误 MN=${frame.header.MN} CN=${frame.header.CN}`);
            return codec_1.Hj212Codec.ack(frame, 3, this.ackMode());
        }
        await this.persist(frame, source);
        this.stat.accepted++;
        return codec_1.Hj212Codec.ack(frame, 1, this.ackMode());
    }
    async persist(frame, source) {
        try {
            await this.frames.insert({
                mn: frame.header.MN,
                cn: frame.header.CN,
                qn: frame.header.QN,
                crcOk: frame.crcOk ? 1 : 0,
                source,
                raw: frame.raw,
                params: frame.params,
            });
        }
        catch (e) {
            this.logger.error(`报文落库失败 MN=${frame.header.MN}: ${e.message}`);
        }
    }
    ackMode() {
        return this.config.get('HJ212_ACK_MODE', 'qnrtn') === 'exerntn' ? 'exerntn' : 'qnrtn';
    }
};
exports.IngestService = IngestService;
exports.IngestService = IngestService = IngestService_1 = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(hj212_frame_entity_1.Hj212Frame)),
    __metadata("design:paramtypes", [typeorm_2.Repository,
        config_1.ConfigService])
], IngestService);
//# sourceMappingURL=ingest.service.js.map