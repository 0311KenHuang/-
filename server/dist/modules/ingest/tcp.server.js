"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var TcpServer_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.TcpServer = void 0;
const common_1 = require("@nestjs/common");
const config_1 = require("@nestjs/config");
const node_net_1 = require("node:net");
const codec_1 = require("./hj212/codec");
const ingest_service_1 = require("./ingest.service");
let TcpServer = TcpServer_1 = class TcpServer {
    config;
    ingest;
    logger = new common_1.Logger(TcpServer_1.name);
    server;
    splitters = new WeakMap();
    idleTimers = new WeakMap();
    constructor(config, ingest) {
        this.config = config;
        this.ingest = ingest;
    }
    onApplicationBootstrap() {
        if (this.config.get('INGEST_MODE', 'direct') !== 'direct') {
            this.logger.log('INGEST_MODE != direct，跳过 TCP 接入（走上级平台对接）');
            return;
        }
        const port = Number(this.config.get('HJ212_TCP_PORT', 20000));
        this.server = (0, node_net_1.createServer)((socket) => this.onConnection(socket));
        this.server.on('error', (e) => this.logger.error(`TCP 服务异常: ${e.message}`));
        this.server.listen(port, '0.0.0.0', () => {
            this.logger.log(`HJ212 TCP 接入已启动 :${port}`);
        });
    }
    onApplicationShutdown() {
        this.server?.close();
    }
    onConnection(socket) {
        const peer = `${socket.remoteAddress}:${socket.remotePort}`;
        this.logger.log(`设备接入 ${peer}`);
        this.splitters.set(socket, new codec_1.FrameSplitter());
        this.touch(socket);
        socket.on('data', (chunk) => {
            this.touch(socket);
            const splitter = this.splitters.get(socket);
            if (!splitter)
                return;
            for (const raw of splitter.push(chunk)) {
                void this.ingest
                    .handleRaw(raw, 'TCP')
                    .then((ack) => {
                    if (ack && !socket.destroyed)
                        socket.write(ack);
                })
                    .catch((e) => this.logger.error(`报文处理异常: ${e.message}`));
            }
        });
        socket.on('error', (e) => this.logger.warn(`连接异常 ${peer}: ${e.message}`));
        socket.on('close', () => {
            this.logger.log(`设备断开 ${peer}`);
            const timer = this.idleTimers.get(socket);
            if (timer)
                clearTimeout(timer);
            this.splitters.delete(socket);
        });
    }
    touch(socket) {
        const old = this.idleTimers.get(socket);
        if (old)
            clearTimeout(old);
        this.idleTimers.set(socket, setTimeout(() => socket.destroy(), 300_000));
    }
};
exports.TcpServer = TcpServer;
exports.TcpServer = TcpServer = TcpServer_1 = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [config_1.ConfigService,
        ingest_service_1.IngestService])
], TcpServer);
//# sourceMappingURL=tcp.server.js.map