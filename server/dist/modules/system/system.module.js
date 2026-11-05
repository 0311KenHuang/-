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
exports.SystemModule = exports.SystemController = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const sys_user_entity_1 = require("../../database/entities/sys-user.entity");
const sys_role_entity_1 = require("../../database/entities/sys-role.entity");
const request_context_1 = require("../../common/context/request.context");
const decorators_1 = require("../../common/decorators");
let SystemController = class SystemController {
    users;
    roles;
    constructor(users, roles) {
        this.users = users;
        this.roles = roles;
    }
    async listUsers(page = '1', pageSize = '20') {
        const tenantId = request_context_1.Ctx.tenantId();
        const [list, total] = await this.users.findAndCount({
            where: { tenantId, deleted: 0 },
            order: { id: 'DESC' },
            skip: (Number(page) - 1) * Number(pageSize),
            take: Math.min(Number(pageSize), 200),
        });
        return {
            list: list.map(({ passwordHash: _p, ...rest }) => rest),
            total,
            page: Number(page),
            pageSize: Number(pageSize),
        };
    }
    listRoles() {
        return this.roles.find({ where: { tenantId: request_context_1.Ctx.tenantId(), deleted: 0 } });
    }
};
exports.SystemController = SystemController;
__decorate([
    (0, common_1.Get)('user'),
    (0, decorators_1.Roles)('ORG', 'REG'),
    __param(0, (0, common_1.Query)('page')),
    __param(1, (0, common_1.Query)('pageSize')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, Object]),
    __metadata("design:returntype", Promise)
], SystemController.prototype, "listUsers", null);
__decorate([
    (0, common_1.Get)('role'),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", void 0)
], SystemController.prototype, "listRoles", null);
exports.SystemController = SystemController = __decorate([
    (0, common_1.Controller)('system'),
    __param(0, (0, typeorm_1.InjectRepository)(sys_user_entity_1.SysUser)),
    __param(1, (0, typeorm_1.InjectRepository)(sys_role_entity_1.SysRole)),
    __metadata("design:paramtypes", [typeorm_2.Repository,
        typeorm_2.Repository])
], SystemController);
let SystemModule = class SystemModule {
};
exports.SystemModule = SystemModule;
exports.SystemModule = SystemModule = __decorate([
    (0, common_1.Module)({
        imports: [typeorm_1.TypeOrmModule.forFeature([sys_user_entity_1.SysUser, sys_role_entity_1.SysRole])],
        controllers: [SystemController],
    })
], SystemModule);
//# sourceMappingURL=system.module.js.map