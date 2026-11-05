"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.AuthService = void 0;
const common_1 = require("@nestjs/common");
const jwt_1 = require("@nestjs/jwt");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const bcrypt = __importStar(require("bcryptjs"));
const sys_user_entity_1 = require("../../database/entities/sys-user.entity");
const sys_tenant_entity_1 = require("../../database/entities/sys-tenant.entity");
const SCOPE_RANK = { SELF: 1, ORG: 2, REGION: 3, ALL: 4 };
let AuthService = class AuthService {
    users;
    tenants;
    jwt;
    constructor(users, tenants, jwt) {
        this.users = users;
        this.tenants = tenants;
        this.jwt = jwt;
    }
    async login(dto) {
        const qb = this.users
            .createQueryBuilder('u')
            .where('u.username = :username', { username: dto.username })
            .andWhere('u.deleted = 0');
        if (dto.tenantCode) {
            const tenant = await this.tenants.findOne({
                where: { tenantCode: dto.tenantCode, deleted: 0 },
            });
            if (!tenant)
                throw new common_1.UnauthorizedException('租户不存在');
            qb.andWhere('u.tenant_id = :tenantId', { tenantId: tenant.id });
        }
        const user = await qb.getOne();
        if (!user)
            throw new common_1.UnauthorizedException('用户名或密码错误');
        if (user.status !== 1)
            throw new common_1.UnauthorizedException('账号已停用或锁定');
        const ok = await bcrypt.compare(dto.password, user.passwordHash);
        if (!ok)
            throw new common_1.UnauthorizedException('用户名或密码错误');
        const roles = user.roles.map((r) => r.roleCode);
        const scope = user.roles
            .map((r) => r.dataScope)
            .sort((a, b) => (SCOPE_RANK[b] ?? 0) - (SCOPE_RANK[a] ?? 0))[0] ?? 'SELF';
        const payload = {
            sub: user.id,
            tenantId: user.tenantId,
            username: user.username,
            orgId: user.orgId,
            roles,
            scope: scope,
        };
        await this.users.update(user.id, { lastLoginAt: new Date() });
        return {
            token: await this.jwt.signAsync(payload),
            user: {
                id: user.id,
                username: user.username,
                realName: user.realName,
                orgId: user.orgId,
                roles,
                dataScope: scope,
            },
        };
    }
};
exports.AuthService = AuthService;
exports.AuthService = AuthService = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(sys_user_entity_1.SysUser)),
    __param(1, (0, typeorm_1.InjectRepository)(sys_tenant_entity_1.SysTenant)),
    __metadata("design:paramtypes", [typeorm_2.Repository,
        typeorm_2.Repository,
        jwt_1.JwtService])
], AuthService);
//# sourceMappingURL=auth.service.js.map