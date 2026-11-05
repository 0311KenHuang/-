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
exports.CreateStationDto = exports.QueryStationDto = exports.STATION_BIZ_STATUS = exports.STATION_TYPES = void 0;
const class_transformer_1 = require("class-transformer");
const class_validator_1 = require("class-validator");
exports.STATION_TYPES = [
    'WASTE_WATER',
    'WASTE_GAS',
    'VOC',
    'INCINERATION',
    'AIR',
    'GROUNDWATER',
];
exports.STATION_BIZ_STATUS = ['NORMAL', 'ABNORMAL', 'STOPPED', 'MAINTAINING'];
class QueryStationDto {
    page = 1;
    pageSize = 20;
    enterpriseId;
    regionId;
    stationType;
    bizStatus;
    keyword;
}
exports.QueryStationDto = QueryStationDto;
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_transformer_1.Type)(() => Number),
    (0, class_validator_1.IsInt)(),
    (0, class_validator_1.Min)(1),
    __metadata("design:type", Object)
], QueryStationDto.prototype, "page", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_transformer_1.Type)(() => Number),
    (0, class_validator_1.IsInt)(),
    (0, class_validator_1.Min)(1),
    (0, class_validator_1.Max)(200),
    __metadata("design:type", Object)
], QueryStationDto.prototype, "pageSize", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_transformer_1.Type)(() => Number),
    (0, class_validator_1.IsInt)(),
    __metadata("design:type", Number)
], QueryStationDto.prototype, "enterpriseId", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_transformer_1.Type)(() => Number),
    (0, class_validator_1.IsInt)(),
    __metadata("design:type", Number)
], QueryStationDto.prototype, "regionId", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsIn)(exports.STATION_TYPES),
    __metadata("design:type", String)
], QueryStationDto.prototype, "stationType", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsIn)(exports.STATION_BIZ_STATUS),
    __metadata("design:type", String)
], QueryStationDto.prototype, "bizStatus", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    (0, class_validator_1.MaxLength)(64),
    __metadata("design:type", String)
], QueryStationDto.prototype, "keyword", void 0);
class CreateStationDto {
    stationCode;
    name;
    stationType;
    enterpriseId;
    regionId;
    lng;
    lat;
    address;
    opsOrgId;
    opsEngineerId;
}
exports.CreateStationDto = CreateStationDto;
__decorate([
    (0, class_validator_1.IsString)(),
    (0, class_validator_1.IsNotEmpty)(),
    (0, class_validator_1.MaxLength)(32),
    __metadata("design:type", String)
], CreateStationDto.prototype, "stationCode", void 0);
__decorate([
    (0, class_validator_1.IsString)(),
    (0, class_validator_1.IsNotEmpty)(),
    (0, class_validator_1.MaxLength)(100),
    __metadata("design:type", String)
], CreateStationDto.prototype, "name", void 0);
__decorate([
    (0, class_validator_1.IsIn)(exports.STATION_TYPES),
    __metadata("design:type", String)
], CreateStationDto.prototype, "stationType", void 0);
__decorate([
    (0, class_validator_1.IsInt)(),
    __metadata("design:type", Number)
], CreateStationDto.prototype, "enterpriseId", void 0);
__decorate([
    (0, class_validator_1.IsInt)(),
    __metadata("design:type", Number)
], CreateStationDto.prototype, "regionId", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsLongitude)(),
    __metadata("design:type", String)
], CreateStationDto.prototype, "lng", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsLatitude)(),
    __metadata("design:type", String)
], CreateStationDto.prototype, "lat", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    (0, class_validator_1.MaxLength)(255),
    __metadata("design:type", String)
], CreateStationDto.prototype, "address", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsInt)(),
    __metadata("design:type", Number)
], CreateStationDto.prototype, "opsOrgId", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsInt)(),
    __metadata("design:type", Number)
], CreateStationDto.prototype, "opsEngineerId", void 0);
//# sourceMappingURL=station.dto.js.map