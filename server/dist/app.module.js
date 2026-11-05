"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.AppModule = void 0;
const common_1 = require("@nestjs/common");
const config_1 = require("@nestjs/config");
const typeorm_1 = require("@nestjs/typeorm");
const core_1 = require("@nestjs/core");
const auth_module_1 = require("./modules/auth/auth.module");
const system_module_1 = require("./modules/system/system.module");
const station_module_1 = require("./modules/station/station.module");
const ingest_module_1 = require("./modules/ingest/ingest.module");
const monitor_module_1 = require("./modules/monitor/monitor.module");
const jwt_auth_guard_1 = require("./common/guards/jwt-auth.guard");
const roles_guard_1 = require("./common/guards/roles.guard");
const audit_interceptor_1 = require("./common/interceptors/audit.interceptor");
const context_interceptor_1 = require("./common/interceptors/context.interceptor");
const response_interceptor_1 = require("./common/interceptors/response.interceptor");
const audit_module_1 = require("./common/audit/audit.module");
const health_controller_1 = require("./health.controller");
let AppModule = class AppModule {
};
exports.AppModule = AppModule;
exports.AppModule = AppModule = __decorate([
    (0, common_1.Module)({
        imports: [
            config_1.ConfigModule.forRoot({ isGlobal: true, envFilePath: ['.env'] }),
            typeorm_1.TypeOrmModule.forRootAsync({
                inject: [config_1.ConfigService],
                useFactory: (config) => ({
                    type: 'mysql',
                    host: config.get('DB_HOST', '127.0.0.1'),
                    port: config.get('DB_PORT', 3306),
                    username: config.get('DB_USER', 'root'),
                    password: config.get('DB_PASSWORD', ''),
                    database: config.get('DB_NAME', 'env_mon'),
                    charset: 'utf8mb4',
                    timezone: '+08:00',
                    autoLoadEntities: true,
                    synchronize: config.get('DB_SYNCHRONIZE', 'false') === 'true',
                    logging: config.get('DB_LOGGING', 'false') === 'true',
                    poolSize: 10,
                }),
            }),
            audit_module_1.AuditModule,
            auth_module_1.AuthModule,
            system_module_1.SystemModule,
            station_module_1.StationModule,
            ingest_module_1.IngestModule,
            monitor_module_1.MonitorModule,
        ],
        controllers: [health_controller_1.HealthController],
        providers: [
            { provide: core_1.APP_GUARD, useClass: jwt_auth_guard_1.JwtAuthGuard },
            { provide: core_1.APP_GUARD, useClass: roles_guard_1.RolesGuard },
            { provide: core_1.APP_INTERCEPTOR, useClass: context_interceptor_1.ContextInterceptor },
            { provide: core_1.APP_INTERCEPTOR, useClass: response_interceptor_1.ResponseInterceptor },
            { provide: core_1.APP_INTERCEPTOR, useClass: audit_interceptor_1.AuditInterceptor },
        ],
    })
], AppModule);
//# sourceMappingURL=app.module.js.map