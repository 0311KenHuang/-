package com.envmon.ingest.hj212;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/** 解析后的报文 */
@Data
public class Hj212Message {

    private String qn;
    private String st;
    private String cn;
    private String pw;
    private String mn;
    private String flag;
    private String pn;
    /** CP 原文，不含 && */
    private String cp;
    /** CP 键值对 */
    private Map<String, String> params = new LinkedHashMap<String, String>();
    private String crc;
    private boolean crcOk;
    private String raw;
}
