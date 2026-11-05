"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
require("reflect-metadata");
const common_1 = require("@nestjs/common");
const core_1 = require("@nestjs/core");
const config_1 = require("@nestjs/config");
const app_module_1 = require("./app.module");
const all_exception_filter_1 = require("./common/filters/all-exception.filter");
async function bootstrap() {
    const app = await core_1.NestFactory.create(app_module_1.AppModule, {
        logger: ['error', 'warn', 'log'],
    });
    const config = app.get(config_1.ConfigService);
    const prefix = config.get('API_PREFIX', 'api');
    app.setGlobalPrefix(prefix, { exclude: ['health', 'health/(.*)'] });
    app.enableCors({ origin: true, credentials: true });
    app.useGlobalPipes(new common_1.ValidationPipe({
        whitelist: true,
        transform: true,
        forbidNonWhitelisted: false,
        transformOptions: { enableImplicitConversion: true },
    }));
    app.useGlobalFilters(new all_exception_filter_1.AllExceptionFilter());
    const port = Number(config.get('PORT', 8080));
    await app.listen(port, '0.0.0.0');
    new common_1.Logger('Bootstrap').log(`server listening on http://127.0.0.1:${port}/${prefix}`);
}
void bootstrap();
//# sourceMappingURL=main.js.map