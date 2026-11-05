import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { APP_GUARD, APP_INTERCEPTOR } from '@nestjs/core';

import { AuthModule } from './modules/auth/auth.module';
import { SystemModule } from './modules/system/system.module';
import { StationModule } from './modules/station/station.module';
import { IngestModule } from './modules/ingest/ingest.module';
import { MonitorModule } from './modules/monitor/monitor.module';

import { JwtAuthGuard } from './common/guards/jwt-auth.guard';
import { RolesGuard } from './common/guards/roles.guard';
import { AuditInterceptor } from './common/interceptors/audit.interceptor';
import { ContextInterceptor } from './common/interceptors/context.interceptor';
import { ResponseInterceptor } from './common/interceptors/response.interceptor';
import { AuditModule } from './common/audit/audit.module';
import { HealthController } from './health.controller';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true, envFilePath: ['.env'] }),
    TypeOrmModule.forRootAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        type: 'mysql' as const,
        host: config.get<string>('DB_HOST', '127.0.0.1'),
        port: config.get<number>('DB_PORT', 3306),
        username: config.get<string>('DB_USER', 'root'),
        password: config.get<string>('DB_PASSWORD', ''),
        database: config.get<string>('DB_NAME', 'env_mon'),
        charset: 'utf8mb4',
        timezone: '+08:00',
        autoLoadEntities: true,
        // 生产环境必须关闭，改用 migration
        synchronize: config.get<string>('DB_SYNCHRONIZE', 'false') === 'true',
        logging: config.get<string>('DB_LOGGING', 'false') === 'true',
        poolSize: 10,
      }),
    }),
    AuditModule,
    AuthModule,
    SystemModule,
    StationModule,
    IngestModule,
    MonitorModule,
  ],
  controllers: [HealthController],
  providers: [
    { provide: APP_GUARD, useClass: JwtAuthGuard },
    { provide: APP_GUARD, useClass: RolesGuard },
    // 顺序即洋葱顺序：上下文最外层，响应包装其次，审计最内层
    { provide: APP_INTERCEPTOR, useClass: ContextInterceptor },
    { provide: APP_INTERCEPTOR, useClass: ResponseInterceptor },
    { provide: APP_INTERCEPTOR, useClass: AuditInterceptor },
  ],
})
export class AppModule {}
