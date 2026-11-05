import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { SysAuditLog } from '../../database/entities/sys-audit-log.entity';
import { AuditService } from './audit.service';

@Module({
  imports: [TypeOrmModule.forFeature([SysAuditLog])],
  providers: [AuditService],
  exports: [AuditService],
})
export class AuditModule {}
