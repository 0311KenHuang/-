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
exports.AuditInterceptor = void 0;
const common_1 = require("@nestjs/common");
const core_1 = require("@nestjs/core");
const rxjs_1 = require("rxjs");
const decorators_1 = require("../decorators");
const audit_service_1 = require("../audit/audit.service");
let AuditInterceptor = class AuditInterceptor {
    reflector;
    audit;
    constructor(reflector, audit) {
        this.reflector = reflector;
        this.audit = audit;
    }
    intercept(context, next) {
        const meta = this.reflector.getAllAndOverride(decorators_1.AUDIT_KEY, [
            context.getHandler(),
            context.getClass(),
        ]);
        if (!meta)
            return next.handle();
        const req = context.switchToHttp().getRequest();
        return next.handle().pipe((0, rxjs_1.tap)((result) => {
            this.audit.record({
                module: meta.module,
                action: meta.action,
                bizId: result?.id ?? req.params?.id ?? null,
                after: req.body,
                ip: req.ip,
                ua: req.headers?.['user-agent'] ?? null,
            });
        }));
    }
};
exports.AuditInterceptor = AuditInterceptor;
exports.AuditInterceptor = AuditInterceptor = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [core_1.Reflector,
        audit_service_1.AuditService])
], AuditInterceptor);
//# sourceMappingURL=audit.interceptor.js.map