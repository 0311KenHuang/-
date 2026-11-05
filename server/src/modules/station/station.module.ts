import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { MdStation } from '../../database/entities/md-station.entity';
import { MdEnterprise } from '../../database/entities/md-enterprise.entity';
import { MdDevice } from '../../database/entities/md-device.entity';
import { MdStationFactor } from '../../database/entities/md-station-factor.entity';
import { StationService } from './station.service';
import { StationController } from './station.controller';

@Module({
  imports: [TypeOrmModule.forFeature([MdStation, MdEnterprise, MdDevice, MdStationFactor])],
  controllers: [StationController],
  providers: [StationService],
  exports: [StationService],
})
export class StationModule {}
