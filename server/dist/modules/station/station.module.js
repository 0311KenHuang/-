"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.StationModule = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const md_station_entity_1 = require("../../database/entities/md-station.entity");
const md_enterprise_entity_1 = require("../../database/entities/md-enterprise.entity");
const md_device_entity_1 = require("../../database/entities/md-device.entity");
const md_station_factor_entity_1 = require("../../database/entities/md-station-factor.entity");
const station_service_1 = require("./station.service");
const station_controller_1 = require("./station.controller");
let StationModule = class StationModule {
};
exports.StationModule = StationModule;
exports.StationModule = StationModule = __decorate([
    (0, common_1.Module)({
        imports: [typeorm_1.TypeOrmModule.forFeature([md_station_entity_1.MdStation, md_enterprise_entity_1.MdEnterprise, md_device_entity_1.MdDevice, md_station_factor_entity_1.MdStationFactor])],
        controllers: [station_controller_1.StationController],
        providers: [station_service_1.StationService],
        exports: [station_service_1.StationService],
    })
], StationModule);
//# sourceMappingURL=station.module.js.map