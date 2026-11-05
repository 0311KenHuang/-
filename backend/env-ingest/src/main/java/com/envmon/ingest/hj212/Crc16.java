package com.envmon.ingest.hj212;

/**
 * CRC16/MODBUS（多项式 0xA001，初值 0xFFFF）。
 * HJ 212 报文尾部的 4 位十六进制即该值，高位在前。
 */
public final class Crc16 {

    private Crc16() {
    }

    public static int of(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 1) == 1) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return crc & 0xFFFF;
    }

    public static int of(String ascii) {
        return of(ascii.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** 4 位大写十六进制 */
    public static String hex(byte[] data) {
        return String.format("%04X", of(data));
    }

    public static String hex(String ascii) {
        return String.format("%04X", of(ascii));
    }
}
