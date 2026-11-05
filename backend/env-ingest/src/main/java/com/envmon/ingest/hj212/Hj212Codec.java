package com.envmon.ingest.hj212;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HJ 212 报文编解码。
 *
 * 结构：## + 数据段长度(4 位十进制，字节数) + 数据段 + CRC16(4 位大写十六进制)
 * 数据段：QN=..;ST=..;CN=..;PW=..;MN=..;Flag=..;PN=..;CP=&&...&&
 *
 * 注意：长度按"字节"计算，不是字符数；含中文时两者不等，所以解码走 byte[]。
 */
public final class Hj212Codec {

    private static final String START = "##";
    private static final int HEAD_MAX = 6;

    private Hj212Codec() {
    }

    public static String encode(Hj212Message m) {
        return encode(m.getQn(), m.getSt(), m.getCn(), m.getPw(), m.getMn(),
                m.getFlag(), m.getPn(), m.getCp());
    }

    public static String encode(String qn, String st, String cn, String pw,
                                String mn, String flag, String pn, String cp) {
        StringBuilder sb = new StringBuilder();
        sb.append("QN=").append(qn)
                .append(";ST=").append(st)
                .append(";CN=").append(cn)
                .append(";PW=").append(pw)
                .append(";MN=").append(mn)
                .append(";Flag=").append(flag);
        if (StringUtils.isNotBlank(pn)) {
            sb.append(";PN=").append(pn);
        }
        sb.append(";CP=&&").append(cp).append("&&");

        String body = sb.toString();
        int len = body.getBytes(StandardCharsets.UTF_8).length;
        return START + String.format("%04d", len) + body + Crc16.hex(body);
    }

    /** 解析一条完整报文；结构非法返回 null，CRC 不匹配时 crcOk=false */
    public static Hj212Message decode(String raw) {
        if (raw == null) {
            return null;
        }
        Hj212Message m = decode(raw.getBytes(StandardCharsets.UTF_8));
        if (m != null) {
            m.setRaw(raw.trim());
        }
        return m;
    }

    public static Hj212Message decode(byte[] data) {
        if (data == null || data.length < HEAD_MAX + 4) {
            return null;
        }
        if (data[0] != '#' || data[1] != '#') {
            return null;
        }
        String lenStr = new String(data, 2, 4, StandardCharsets.US_ASCII);
        if (!lenStr.matches("\\d{4}")) {
            return null;
        }
        int bodyLen = Integer.parseInt(lenStr);
        int bodyStart = HEAD_MAX;
        int bodyEnd = bodyStart + bodyLen;
        if (data.length < bodyEnd + 4) {
            return null;
        }

        String body = new String(data, bodyStart, bodyLen, StandardCharsets.UTF_8);
        String crc = new String(data, bodyEnd, 4, StandardCharsets.US_ASCII).toUpperCase();

        int cpIdx = body.indexOf("CP=");
        if (cpIdx < 0) {
            return null;
        }
        String headerPart = body.substring(0, cpIdx);
        String cpPart = body.substring(cpIdx + 3);
        if (cpPart.startsWith("&&")) {
            cpPart = cpPart.substring(2);
        }
        if (cpPart.endsWith("&&")) {
            cpPart = cpPart.substring(0, cpPart.length() - 2);
        }

        Map<String, String> header = new LinkedHashMap<String, String>();
        for (String seg : headerPart.split(";")) {
            int i = seg.indexOf('=');
            if (i > 0) {
                header.put(seg.substring(0, i).trim(), seg.substring(i + 1).trim());
            }
        }

        Hj212Message m = new Hj212Message();
        m.setQn(header.get("QN"));
        m.setSt(header.get("ST"));
        m.setCn(header.get("CN"));
        m.setPw(header.get("PW"));
        m.setMn(header.get("MN"));
        m.setFlag(header.get("Flag"));
        m.setPn(header.get("PN"));
        m.setCp(cpPart);
        m.setParams(parseParams(cpPart));
        m.setCrc(crc);
        m.setCrcOk(Crc16.hex(body).equalsIgnoreCase(crc));
        return m;
    }

    /** CP 解析：先按 ; 分对，值内部可能再用 , 分隔多参数 */
    public static Map<String, String> parseParams(String cp) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        if (cp == null) {
            return out;
        }
        String lastKey = null;
        for (String seg : cp.split(";")) {
            if (seg.isEmpty()) {
                continue;
            }
            for (String token : seg.split(",")) {
                String t = token.trim();
                if (t.isEmpty()) {
                    continue;
                }
                int eq = t.indexOf('=');
                if (eq > 0) {
                    String key = t.substring(0, eq).trim();
                    out.put(key, t.substring(eq + 1).trim());
                    lastKey = key;
                } else if (lastKey != null) {
                    out.put(lastKey, out.get(lastKey) + "," + t);
                }
            }
        }
        return out;
    }

    /** 应答：数据上传回 QnRtn，控制命令回 ExeRtn */
    public static String ack(Hj212Message m, int result, String mode) {
        String cp = "exerntn".equalsIgnoreCase(mode) ? ("ExeRtn=" + result) : ("QnRtn=" + result);
        return encode(m.getQn(), m.getSt(), m.getCn(), m.getPw(), m.getMn(), m.getFlag(), null, cp);
    }

    /** QN：yyyyMMddHHmmssSSS + 3 位序号 */
    public static String nextQn(int seq) {
        String ts = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        return ts + String.format("%03d", seq % 1000);
    }
}
