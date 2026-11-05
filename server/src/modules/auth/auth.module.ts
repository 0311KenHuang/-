import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { JwtModule } from '@nestjs/jwt';
import { TypeOrmModule } from '@nestjs/typeorm';
import { SysUser } from '../../database/entities/sys-user.entity';
import { SysTenant } from '../../database/entities/sys-tenant.entity';
import { AuthService } from './auth.service';
import { AuthController } from './auth.controller';

@Module({
  imports: [
    TypeOrmModule.forFeature([SysUser, SysTenant]),
    JwtModule.registerAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        secret: config.get<string>('JWT_SECRET', 'dev-secret'),
        // expiresIn 的类型来自 ms 的 StringValue 字面量联合，配置值是运行时字符串，这里显式放宽
        signOptions: { expiresIn: (config.get<string>('JWT_EXPIRES_IN') ?? '8h') as unknown as number },
      }),
    }),
  ],
  controllers: [AuthController],
  providers: [AuthService],
  exports: [AuthService, JwtModule],
})
export class AuthModule {}
