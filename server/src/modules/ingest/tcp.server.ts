import {
  Injectable,
  Logger,
  OnApplicationBootstrap,
  OnApplicationShutdown,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createServer, Server, Socket } from 'node:net';
import { FrameSplitter } from './hj212/codec';
import { IngestService } from './ingest.service';

/**
 * HJ 212 TCP 服务端（现场机主动连上来）。
 *
 * 关键约束：
 * 1) 先应答后落库 —— 现场机超时重发会把平台打爆；
 * 2) 每个连接独立拆包器 —— 粘包/半包按 "##+长度" 切；
 * 3) 空闲超时断开 —— 防止半开连接占满句柄。
 */
@Injectable()
export class TcpServer implements OnApplicationBootstrap, OnApplicationShutdown {
  private readonly logger = new Logger(TcpServer.name);
  private server?: Server;
  private readonly splitters = new WeakMap<Socket, FrameSplitter>();
  private readonly idleTimers = new WeakMap<Socket, NodeJS.Timeout>();

  constructor(
    private readonly config: ConfigService,
    private readonly ingest: IngestService,
  ) {}

  onApplicationBootstrap(): void {
    if (this.config.get('INGEST_MODE', 'direct') !== 'direct') {
      this.logger.log('INGEST_MODE != direct，跳过 TCP 接入（走上级平台对接）');
      return;
    }
    const port = Number(this.config.get('HJ212_TCP_PORT', 20000));

    this.server = createServer((socket) => this.onConnection(socket));
    this.server.on('error', (e) => this.logger.error(`TCP 服务异常: ${e.message}`));
    this.server.listen(port, '0.0.0.0', () => {
      this.logger.log(`HJ212 TCP 接入已启动 :${port}`);
    });
  }

  onApplicationShutdown(): void {
    this.server?.close();
  }

  private onConnection(socket: Socket): void {
    const peer = `${socket.remoteAddress}:${socket.remotePort}`;
    this.logger.log(`设备接入 ${peer}`);
    this.splitters.set(socket, new FrameSplitter());
    this.touch(socket);

    socket.on('data', (chunk) => {
      this.touch(socket);
      const splitter = this.splitters.get(socket);
      if (!splitter) return;

      for (const raw of splitter.push(chunk)) {
        void this.ingest
          .handleRaw(raw, 'TCP')
          .then((ack) => {
            if (ack && !socket.destroyed) socket.write(ack);
          })
          .catch((e: Error) => this.logger.error(`报文处理异常: ${e.message}`));
      }
    });

    socket.on('error', (e) => this.logger.warn(`连接异常 ${peer}: ${e.message}`));
    socket.on('close', () => {
      this.logger.log(`设备断开 ${peer}`);
      const timer = this.idleTimers.get(socket);
      if (timer) clearTimeout(timer);
      this.splitters.delete(socket);
    });
  }

  /** 空闲 300s 断开，对应标准的心跳约束 */
  private touch(socket: Socket): void {
    const old = this.idleTimers.get(socket);
    if (old) clearTimeout(old);
    this.idleTimers.set(
      socket,
      setTimeout(() => socket.destroy(), 300_000),
    );
  }
}
