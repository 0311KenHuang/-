package com.envmon.ingest.netty;

import com.envmon.ingest.hj212.Hj212FrameDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * HJ212 TCP 服务端。
 * 独立进程部署：长连接会长期占用线程与堆外内存，和大盘 Web 抢资源不划算。
 *
 * 业务 Handler 挂在独立 DefaultEventExecutorGroup 上：
 * 落库是同步 JDBC，绝不能跑在 event loop 线程——否则数据库一抖，
 * 所有设备连接的读写事件全部停摆。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Hj212Server {

    private final Hj212ChannelHandler businessHandler;

    @Value("${hj212.port:20000}")
    private int port;

    @Value("${hj212.idle-seconds:300}")
    private int idleSeconds;

    @Value("${hj212.business-threads:16}")
    private int businessThreads;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private DefaultEventExecutorGroup businessGroup;
    private Channel serverChannel;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        businessGroup = new DefaultEventExecutorGroup(businessThreads);

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new IdleStateHandler(idleSeconds, 0, 0, TimeUnit.SECONDS))
                                .addLast(new Hj212FrameDecoder())
                                .addLast(new StringEncoder(CharsetUtil.UTF_8))
                                // 关键：业务 handler 传入 executorGroup，
                                // channelRead0 及其后的落库/应答都在业务线程池执行
                                .addLast(businessGroup, businessHandler);
                    }
                });

        try {
            serverChannel = bootstrap.bind(port).sync().channel();
            log.info("HJ212 TCP 接入已启动 :{}（业务线程 {}）", port, businessThreads);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("HJ212 服务启动失败", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (businessGroup != null) {
            businessGroup.shutdownGracefully();
        }
        log.info("HJ212 TCP 接入已停止");
    }
}
