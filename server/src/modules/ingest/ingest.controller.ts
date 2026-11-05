import { Body, Controller, Get, Headers, Post } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { IngestService } from './ingest.service';
import { Public } from '../../common/decorators';
import { BizException, ErrCode } from '../../common/errors/biz.exception';

@Controller('ingest')
export class IngestController {
  constructor(
    private readonly ingest: IngestService,
    private readonly config: ConfigService,
  ) {}

  /**
   * HTTP 上报入口（部分数采仪只支持 HTTP）。
   * 设备无法携带 JWT，用共享令牌鉴权：Header X-Ingest-Token。
   */
  @Public()
  @Post('hj212')
  async upload(
    @Body() body: string | { raw: string },
    @Headers('x-ingest-token') token?: string,
  ) {
    const expected = this.config.get<string>('INGEST_TOKEN');
    if (expected && token !== expected) {
      throw new BizException(ErrCode.UNAUTHORIZED, '接入令牌无效', 401);
    }
    const raw = typeof body === 'string' ? body : body?.raw;
    if (!raw) throw new BizException(ErrCode.BAD_REQUEST, '报文为空');

    const ack = await this.ingest.handleRaw(raw, 'HTTP');
    return { ack };
  }

  @Get('stat')
  stat() {
    return this.ingest.stat;
  }
}
