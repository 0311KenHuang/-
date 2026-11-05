import { Body, Controller, Get, Param, ParseIntPipe, Patch, Post, Query } from '@nestjs/common';
import { StationService } from './station.service';
import { CreateStationDto, QueryStationDto } from './dto/station.dto';
import { Audit, Roles } from '../../common/decorators';

@Controller('station')
export class StationController {
  constructor(private readonly service: StationService) {}

  @Get()
  page(@Query() query: QueryStationDto) {
    return this.service.page(query);
  }

  @Get(':id')
  detail(@Param('id', ParseIntPipe) id: number) {
    return this.service.detail(id);
  }

  @Post()
  @Roles('ORG', 'MGR')
  @Audit('station', 'CREATE')
  create(@Body() dto: CreateStationDto) {
    return this.service.create(dto);
  }

  @Patch(':id')
  @Roles('ORG', 'MGR')
  @Audit('station', 'UPDATE')
  update(@Param('id', ParseIntPipe) id: number, @Body() dto: Partial<CreateStationDto>) {
    return this.service.update(id, dto);
  }

  @Patch(':id/status')
  @Roles('ORG', 'MGR', 'REG')
  @Audit('station', 'CHANGE_STATUS')
  changeStatus(
    @Param('id', ParseIntPipe) id: number,
    @Body() body: { bizStatus: string; stopReason?: string },
  ) {
    return this.service.changeStatus(id, body.bizStatus, body.stopReason);
  }
}
