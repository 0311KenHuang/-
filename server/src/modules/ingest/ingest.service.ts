import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Hj212Frame } from '../../database/entities/hj212-frame.entity';
import { Hj212Codec, Hj212Frame as ParsedFrame } from './hj212/codec';

export interface IngestStat {
  received: number;
  accepted: number;
  crcFailed: number;
  parseFailed: number;
}

/**
 * 接入编排：解析 → 落库 → 分发。
 *
 * 这里刻意不做业务计算（附录 G 判定、报警），只负责"把报文变成可信事件"，
 * 计算交给下游消费者，避免接入层被慢逻辑拖死。
 */
@Injectable()
export class IngestService {
  private readonly logger = new Logger(IngestService.name);
  readonly stat: IngestStat = { received: 0, accepted: 0, crcFailed: 0, parseFailed: 0 };

  constructor(
    @InjectRepository(Hj212Frame) private readonly frames: Repository<Hj212Frame>,
    private readonly config: ConfigService,
  ) {}

  /** 处理一条原始报文，返回应答报文（无效应答 result=2） */
  async handleRaw(raw: string, source: 'TCP' | 'HTTP'): Promise<string | null> {
    this.stat.received++;

    const frame = Hj212Codec.decode(raw);
    if (!frame) {
      this.stat.parseFailed++;
      return null;
    }

    if (!frame.crcOk) {
      this.stat.crcFailed++;
      this.logger.warn(`CRC 错误 MN=${frame.header.MN} CN=${frame.header.CN}`);
      return Hj212Codec.ack(frame, 3, this.ackMode());
    }

    await this.persist(frame, source);
    this.stat.accepted++;
    return Hj212Codec.ack(frame, 1, this.ackMode());
  }

  private async persist(frame: ParsedFrame, source: 'TCP' | 'HTTP'): Promise<void> {
    try {
      await this.frames.insert({
        mn: frame.header.MN,
        cn: frame.header.CN,
        qn: frame.header.QN,
        crcOk: frame.crcOk ? 1 : 0,
        source,
        raw: frame.raw,
        params: frame.params,
      });
    } catch (e) {
      // 落库失败不能丢包：先记日志，后续可接入本地重试队列
      this.logger.error(`报文落库失败 MN=${frame.header.MN}: ${(e as Error).message}`);
    }
  }

  private ackMode(): 'qnrtn' | 'exerntn' {
    return this.config.get('HJ212_ACK_MODE', 'qnrtn') === 'exerntn' ? 'exerntn' : 'qnrtn';
  }
}
