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
exports.MonitorModule = exports.MonitorController = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const md_station_entity_1 = require("../../database/entities/md-station.entity");
const alm_event_entity_1 = require("../../database/entities/alm-event.entity");
const hj212_frame_entity_1 = require("../../database/entities/hj212-frame.entity");
const monitor_service_1 = require("./monitor.service");
let MonitorController = class MonitorController {
    service;
    constructor(service) {
        this.service = service;
    }
    overview() {
        return this.service.overview();
    }
};
exports.MonitorController = MonitorController;
__decorate([
    (0, common_1.Get)('overview'),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", void 0)
], MonitorController.prototype, "overview", null);
exports.MonitorController = MonitorController = __decorate([
    (0, common_1.Controller)('monitor'),
    __metadata("design:paramtypes", [monitor_service_1.MonitorService])
], MonitorController);
let MonitorModule = class MonitorModule {
};
exports.MonitorModule = MonitorModule;
exports.MonitorModule = MonitorModule = __decorate([
    (0, common_1.Module)({
        imports: [typeorm_1.TypeOrmModule.forFeature([md_station_entity_1.MdStation, alm_event_entity_1.AlmEvent, hj212_frame_entity_1.Hj212Frame])],
        controllers: [MonitorController],
        providers: [monitor_service_1.MonitorService],
    })
], MonitorModule);
//# sourceMappingURL=monitor.module.js.map