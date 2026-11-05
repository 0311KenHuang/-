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
var AuditService_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.AuditService = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const sys_audit_log_entity_1 = require("../../database/entities/sys-audit-log.entity");
const request_context_1 = require("../context/request.context");
let AuditService = AuditService_1 = class AuditService {
    repo;
    logger = new common_1.Logger(AuditService_1.name);
    constructor(repo) {
        this.repo = repo;
    }
    record(entry) {
        const ctx = request_context_1.Ctx.get();
        const row = this.repo.create({
            tenantId: ctx?.tenantId ?? 0,
            userId: ctx?.userId ?? 0,
            userName: ctx?.username ?? null,
            module: entry.module,
            action: entry.action,
            bizId: entry.bizId != null ? String(entry.bizId) : null,
            beforeJson: this.sanitize(entry.before),
            afterJson: this.sanitize(entry.after),
            ip: entry.ip ?? null,
            ua: entry.ua ?? null,
        });
        void this.repo.save(row).catch((e) => {
            this.logger.warn(`审计写入失败: ${e.message}`);
        });
    }
    sanitize(input) {
        if (!input || typeof input !== 'object')
            return input ?? null;
        const clone = Array.isArray(input)
            ? { list: input }
            : { ...input };
        for (const key of Object.keys(clone)) {
            if (/password|token|secret|credential/i.test(key))
                clone[key] = '***';
        }
        return clone;
    }
};
exports.AuditService = AuditService;
exports.AuditService = AuditService = AuditService_1 = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(sys_audit_log_entity_1.SysAuditLog)),
    __metadata("design:paramtypes", [typeorm_2.Repository])
], AuditService);
//# sourceMappingURL=audit.service.js.map