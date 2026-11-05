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
exports.StationController = void 0;
const common_1 = require("@nestjs/common");
const station_service_1 = require("./station.service");
const station_dto_1 = require("./dto/station.dto");
const decorators_1 = require("../../common/decorators");
let StationController = class StationController {
    service;
    constructor(service) {
        this.service = service;
    }
    page(query) {
        return this.service.page(query);
    }
    detail(id) {
        return this.service.detail(id);
    }
    create(dto) {
        return this.service.create(dto);
    }
    update(id, dto) {
        return this.service.update(id, dto);
    }
    changeStatus(id, body) {
        return this.service.changeStatus(id, body.bizStatus, body.stopReason);
    }
};
exports.StationController = StationController;
__decorate([
    (0, common_1.Get)(),
    __param(0, (0, common_1.Query)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [station_dto_1.QueryStationDto]),
    __metadata("design:returntype", void 0)
], StationController.prototype, "page", null);
__decorate([
    (0, common_1.Get)(':id'),
    __param(0, (0, common_1.Param)('id', common_1.ParseIntPipe)),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Number]),
    __metadata("design:returntype", void 0)
], StationController.prototype, "detail", null);
__decorate([
    (0, common_1.Post)(),
    (0, decorators_1.Roles)('ORG', 'MGR'),
    (0, decorators_1.Audit)('station', 'CREATE'),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [station_dto_1.CreateStationDto]),
    __metadata("design:returntype", void 0)
], StationController.prototype, "create", null);
__decorate([
    (0, common_1.Patch)(':id'),
    (0, decorators_1.Roles)('ORG', 'MGR'),
    (0, decorators_1.Audit)('station', 'UPDATE'),
    __param(0, (0, common_1.Param)('id', common_1.ParseIntPipe)),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Number, Object]),
    __metadata("design:returntype", void 0)
], StationController.prototype, "update", null);
__decorate([
    (0, common_1.Patch)(':id/status'),
    (0, decorators_1.Roles)('ORG', 'MGR', 'REG'),
    (0, decorators_1.Audit)('station', 'CHANGE_STATUS'),
    __param(0, (0, common_1.Param)('id', common_1.ParseIntPipe)),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Number, Object]),
    __metadata("design:returntype", void 0)
], StationController.prototype, "changeStatus", null);
exports.StationController = StationController = __decorate([
    (0, common_1.Controller)('station'),
    __metadata("design:paramtypes", [station_service_1.StationService])
], StationController);
//# sourceMappingURL=station.controller.js.map