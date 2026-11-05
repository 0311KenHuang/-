package com.envmon.ingest.hj212;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;

/**
 * HJ212 拆包器：按 "## + 4 位长度" 切帧。
 *
 * 处理三类脏数据：
 * 1) 粘包/半包 —— 长度不足就等下一批；
 * 2) 帧头前有垃圾字节 —— 自动对齐；
 * 3) 假帧头（## 后不是 4 位数字）—— 跳过 2 字节继续找。
 */
public class Hj212FrameDecoder extends ByteToMessageDecoder {

    private static final int HEAD_MAX = 6;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        while (true) {
            int start = findHeader(in);
            if (start < 0) {
                // 没找到帧头，保留最后 1 字节以防 "##" 被切开
                int keepFrom = Math.max(in.readerIndex(), in.writerIndex() - 1);
                in.readerIndex(keepFrom);
                return;
            }
            if (start > in.readerIndex()) {
                in.readerIndex(start);
            }
            if (in.readableBytes() < HEAD_MAX) {
                return;
            }

            String lenStr = in.toString(in.readerIndex() + 2, 4, CharsetUtil.US_ASCII);
            if (!lenStr.matches("\\d{4}")) {
                in.readerIndex(in.readerIndex() + 2);
                continue;
            }

            int total = HEAD_MAX + Integer.parseInt(lenStr) + 4;
            if (in.readableBytes() < total) {
                return;
            }

            ByteBuf frame = in.readRetainedSlice(total);
            out.add(frame.toString(CharsetUtil.UTF_8));
        }
    }

    private int findHeader(ByteBuf in) {
        int last = in.writerIndex() - 1;
        for (int i = in.readerIndex(); i < last; i++) {
            if (in.getByte(i) == '#' && in.getByte(i + 1) == '#') {
                return i;
            }
        }
        return -1;
    }
}
