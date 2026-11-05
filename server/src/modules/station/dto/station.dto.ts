import { Type } from 'class-transformer';
import {
  IsIn,
  IsInt,
  IsLatitude,
  IsLongitude,
  IsNotEmpty,
  IsOptional,
  IsString,
  Max,
  MaxLength,
  Min,
} from 'class-validator';

export const STATION_TYPES = [
  'WASTE_WATER',
  'WASTE_GAS',
  'VOC',
  'INCINERATION',
  'AIR',
  'GROUNDWATER',
] as const;

export const STATION_BIZ_STATUS = ['NORMAL', 'ABNORMAL', 'STOPPED', 'MAINTAINING'] as const;

export class QueryStationDto {
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  page = 1;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(200)
  pageSize = 20;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  enterpriseId?: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  regionId?: number;

  @IsOptional()
  @IsIn(STATION_TYPES as unknown as string[])
  stationType?: string;

  @IsOptional()
  @IsIn(STATION_BIZ_STATUS as unknown as string[])
  bizStatus?: string;

  /** 站房名 / 编码 / 企业名 模糊搜索 */
  @IsOptional()
  @IsString()
  @MaxLength(64)
  keyword?: string;
}

export class CreateStationDto {
  @IsString()
  @IsNotEmpty()
  @MaxLength(32)
  stationCode: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  name: string;

  @IsIn(STATION_TYPES as unknown as string[])
  stationType: string;

  @IsInt()
  enterpriseId: number;

  @IsInt()
  regionId: number;

  @IsOptional()
  @IsLongitude()
  lng?: string;

  @IsOptional()
  @IsLatitude()
  lat?: string;

  @IsOptional()
  @IsString()
  @MaxLength(255)
  address?: string;

  @IsOptional()
  @IsInt()
  opsOrgId?: number;

  @IsOptional()
  @IsInt()
  opsEngineerId?: number;
}
