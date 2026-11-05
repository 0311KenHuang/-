package com.envmon.ingest.netty;

import com.envmon.ingest.hj212.Hj212Codec;
import com.envmon.ingest.hj212.Hj212Message;
import com.envmon.ingest.service.FrameService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 业务处理：解析 → 按 MN 归属站房 → 落库 → 应答。
 *
 * 顺序是「先落库后应答」：落库成功才回 QnRtn=1，
 * 落库失败回 QnRtn=3，让现场机按协议超时重发，平台不静默丢数。
 * 落库/应答都在独立业务线程池执行（见 Hj212Server 的 businessGroup），
 * 数据库抖动不会阻塞 event loop 上的其他连接。
 */
@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class Hj212ChannelHandler extends SimpleChannelInboundHandler<String> {

    private final FrameService frameService;

    @Value("${hj212.ack-mode:qnrtn}")
    private String ackMode;

    /** 未注册 MN 是否拒收（默认拒收，避免野设备污染数据） */
    @Value("${hj212.reject-unknown-mn:true}")
    private boolean rejectUnknownMn;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("设备接入 {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("设备断开 {}", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String raw) {
        frameService.received.incrementAndGet();

        Hj212Message m = Hj212Codec.decode(raw);
        if (m == null) {
            frameService.parseFailed.incrementAndGet();
            log.warn("报文结构非法，已丢弃: {}", abbreviate(raw));
            return;
        }
        if (!m.isCrcOk()) {
            frameService.crcFailed.incrementAndGet();
            log.warn("CRC 校验失败 MN={} CN={}", m.getMn(), m.getCn());
            writeAck(ctx, m, 3);
            return;
        }

        long[] owner = frameService.resolve(m.getMn());
        if (owner == null) {
            // 原文仍然落库：将来排查"谁在往我们这里发数据"时是唯一线索
            boolean stored = tryPersist(m, null);
            if (rejectUnknownMn) {
                log.warn("未注册 MN={}，已拒收（原文{}）", m.getMn(), stored ? "已留档" : "留档失败");
                writeAck(ctx, m, 2);
                return;
            }
            if (!stored) {
                // 原文都没存下来，回错误让设备重发，否则这帧永远找不回来
                writeAck(ctx, m, 3);
                return;
            }
            log.warn("未注册 MN={}，已接收但未归属站房", m.getMn());
        } else {
            if (!tryPersist(m, owner)) {
                // P0 修复：落库失败必须回错误应答（QnRtn=3），
                // 现场机会按协议重发；之前吞异常后仍回成功，设备不重发 → 平台静默丢数
                writeAck(ctx, m, 3);
                return;
            }
            frameService.accepted.incrementAndGet();
        }
        writeAck(ctx, m, 1);
    }

    /**
     * 落库（不抛异常）。
     *
     * @return true 落库成功；false 失败（已记日志，调用方应回错误应答）
     */
    private boolean tryPersist(Hj212Message m, long[] owner) {
        try {
            frameService.persist(m, "TCP", owner);
            return true;
        } catch (Exception e) {
            log.error("报文落库失败 MN={}: {}", m.getMn(), e.getMessage());
            return false;
        }
    }

    private void writeAck(ChannelHandlerContext ctx, Hj212Message m, int result) {
        if (ctx.channel().isActive()) {
            ctx.writeAndFlush(Hj212Codec.ack(m, result, ackMode));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("连接异常 {}: {}", ctx.channel().remoteAddress(), cause.getMessage());
        ctx.close();
    }

    private String abbreviate(String s) {
        return s == null || s.length() <= 120 ? s : s.substring(0, 120) + "...";
    }
}
