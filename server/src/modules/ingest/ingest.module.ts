import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Hj212Frame } from '../../database/entities/hj212-frame.entity';
import { IngestService } from './ingest.service';
import { IngestController } from './ingest.controller';
import { TcpServer } from './tcp.server';

@Module({
  imports: [TypeOrmModule.forFeature([Hj212Frame])],
  controllers: [IngestController],
  providers: [IngestService, TcpServer],
  exports: [IngestService],
})
export class IngestModule {}
