import { Controller, Get, Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { MdStation } from '../../database/entities/md-station.entity';
import { AlmEvent } from '../../database/entities/alm-event.entity';
import { Hj212Frame } from '../../database/entities/hj212-frame.entity';
import { MonitorService } from './monitor.service';

@Controller('monitor')
export class MonitorController {
  constructor(private readonly service: MonitorService) {}

  /** 工作台概览 */
  @Get('overview')
  overview() {
    return this.service.overview();
  }
}

@Module({
  imports: [TypeOrmModule.forFeature([MdStation, AlmEvent, Hj212Frame])],
  controllers: [MonitorController],
  providers: [MonitorService],
})
export class MonitorModule {}
