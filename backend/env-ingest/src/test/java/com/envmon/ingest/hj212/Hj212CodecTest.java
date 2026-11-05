package com.envmon.ingest.hj212;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Hj212CodecTest {

    private static final String CP =
            "DataTime=20261104141700;a21026-Rtd=33.215,a21026-Flag=N;a00000-Rtd=42.650,a00000-Flag=N";

    private static String frame() {
        return Hj212Codec.encode("20261104141700001", "32", "2051", "123456",
                "FHZY-S-01DTU01", "4", null, CP);
    }

    @Test
    @DisplayName("CRC16 标准校验值：123456789 -> 0x4B37")
    void crcStandard() {
        assertEquals(0x4B37, Crc16.of("123456789"));
        assertEquals("4B37", Crc16.hex("123456789"));
    }

    @Test
    @DisplayName("CRC 输出恒为 4 位大写十六进制")
    void crcFormat() {
        for (String s : new String[]{"", "##", "A", "中文"}) {
            assertTrue(Crc16.hex(s).matches("^[0-9A-F]{4}$"));
        }
    }

    @Test
    @DisplayName("编码后再解码，字段与参数应完全一致")
    void roundTrip() {
        String raw = frame();
        assertTrue(raw.startsWith("##"));

        Hj212Message m = Hj212Codec.decode(raw);
        assertNotNull(m);
        assertTrue(m.isCrcOk());
        assertEquals("FHZY-S-01DTU01", m.getMn());
        assertEquals("2051", m.getCn());
        assertEquals("20261104141700", m.getParams().get("DataTime"));
        assertEquals("33.215", m.getParams().get("a21026-Rtd"));
        assertEquals("N", m.getParams().get("a21026-Flag"));
    }

    @Test
    @DisplayName("长度字段等于数据段字节数")
    void lengthField() {
        String raw = frame();
        int declared = Integer.parseInt(raw.substring(2, 6));
        String body = raw.substring(6, 6 + declared);
        assertEquals(declared, body.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(4, raw.length() - 6 - declared);
    }

    @Test
    @DisplayName("CRC 被篡改时应能解出结构但 crcOk=false")
    void tamperedCrc() {
        String raw = frame();
        char last = raw.charAt(raw.length() - 1);
        String tampered = raw.substring(0, raw.length() - 1) + (last == '0' ? '1' : '0');
        Hj212Message m = Hj212Codec.decode(tampered);
        assertNotNull(m);
        assertFalse(m.isCrcOk());
    }

    @Test
    @DisplayName("结构非法返回 null")
    void invalid() {
        assertNull(Hj212Codec.decode("随便一段文字"));
        assertNull(Hj212Codec.decode("##12xx"));
        assertNull(Hj212Codec.decode("##0001QN=1"));
    }

    @Test
    @DisplayName("应答报文携带 QnRtn / ExeRtn")
    void ack() {
        Hj212Message m = Hj212Codec.decode(frame());
        assertNotNull(m);

        Hj212Message qn = Hj212Codec.decode(Hj212Codec.ack(m, 1, "qnrtn"));
        assertNotNull(qn);
        assertEquals("20261104141700001", qn.getQn());
        assertEquals("1", qn.getParams().get("QnRtn"));

        Hj212Message exe = Hj212Codec.decode(Hj212Codec.ack(m, 2, "exerntn"));
        assertNotNull(exe);
        assertEquals("2", exe.getParams().get("ExeRtn"));
    }

    @Test
    @DisplayName("拆包器：一次收到两条报文应全部拆出")
    void decoderSticky() {
        String f1 = frame();
        String f2 = Hj212Codec.encode("20261104141800001", "32", "2051", "123456",
                "FHZY-S-01DTU01", "4", null, CP);

        EmbeddedChannel channel = new EmbeddedChannel(new Hj212FrameDecoder());
        channel.writeInbound(io.netty.buffer.Unpooled.copiedBuffer(f1 + f2, StandardCharsets.UTF_8));

        assertEquals(f1, channel.readInbound());
        assertEquals(f2, channel.readInbound());
        assertNull(channel.readInbound());
    }

    @Test
    @DisplayName("拆包器：半包应等待后续数据")
    void decoderHalfPacket() {
        String f = frame();
        int half = f.length() / 2;

        EmbeddedChannel channel = new EmbeddedChannel(new Hj212FrameDecoder());
        channel.writeInbound(io.netty.buffer.Unpooled.copiedBuffer(f.substring(0, half),
                StandardCharsets.UTF_8));
        assertNull(channel.readInbound());

        channel.writeInbound(io.netty.buffer.Unpooled.copiedBuffer(f.substring(half),
                StandardCharsets.UTF_8));
        assertEquals(f, channel.readInbound());
    }

    @Test
    @DisplayName("拆包器：垃圾前缀与假帧头能自动对齐")
    void decoderGarbage() {
        String f = frame();
        EmbeddedChannel channel = new EmbeddedChannel(new Hj212FrameDecoder());
        channel.writeInbound(io.netty.buffer.Unpooled.copiedBuffer("xx##zz" + f,
                StandardCharsets.UTF_8));
        assertEquals(f, channel.readInbound());
    }
}
