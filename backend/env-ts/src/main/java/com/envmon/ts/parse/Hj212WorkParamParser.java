package com.envmon.ts.parse;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作参数 CP 解析（HJ 212—2025 新增上传内容）。
 *
 * 标准里"上传工作参数"的命令编码尚未在正式文本固定，所以由接入层配置
 * hj212.work-param-cn 决定哪些 CN 走这里；解析本身只按参数名白名单识别，
 * 换 CN 不用改代码。
 *
 * 识别两类键：
 *   设备身份：SN / CPUID / MAC / FIRMWARE
 *   工作参数：RANGE_MIN / RANGE_MAX / SLOPE / INTERCEPT / CAL_CONC / DIGEST_TEMP
 * 带因子前缀时（如 a21026-RangeMax）保留为 "a21026:RANGE_MAX"，避免多因子互相覆盖。
 */
public final class Hj212WorkParamParser {

    private static final Map<String, String> ALIASES;

    static {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("SN", "SN");
        m.put("CPUID", "CPUID");
        m.put("MAC", "MAC");
        m.put("FIRMWARE", "FIRMWARE");
        m.put("FIRMWAREVERSION", "FIRMWARE");
        m.put("RANGEMIN", "RANGE_MIN");
        m.put("RANGEMAX", "RANGE_MAX");
        m.put("SLOPE", "SLOPE");
        m.put("INTERCEPT", "INTERCEPT");
        m.put("CALCONC", "CAL_CONC");
        m.put("CALIBRATIONCONC", "CAL_CONC");
        m.put("DIGESTTEMP", "DIGEST_TEMP");
        ALIASES = Collections.unmodifiableMap(m);
    }

    private Hj212WorkParamParser() {
    }

    /** @return 归一化参数编码；不是工作参数返回 null */
    public static String canonical(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        String raw = key.trim();
        String prefix = null;
        String tail = raw;
        int dash = raw.lastIndexOf('-');
        if (dash > 0) {
            prefix = raw.substring(0, dash).trim();
            tail = raw.substring(dash + 1).trim();
        }
        String normalized = tail.replace("_", "").replace("-", "").replace(" ", "").toUpperCase();
        String code = ALIASES.get(normalized);
        if (code == null) {
            return null;
        }
        return prefix == null || prefix.isEmpty() ? code : prefix + ":" + code;
    }

    public static boolean isParamKey(String key) {
        return canonical(key) != null;
    }

    /** 解析 CP 原文；时间取 DataTime，缺失时退回 QN 前 14 位 */
    public static List<WorkParamPoint> parse(String cp, String qn, LocalDateTime fallbackTime) {
        List<WorkParamPoint> out = new ArrayList<WorkParamPoint>();
        if (StringUtils.isBlank(cp)) {
            return out;
        }
        LocalDateTime defaultTime = Hj212DataParser.timeFromQn(qn);
        if (defaultTime == null && fallbackTime != null) {
            defaultTime = fallbackTime.truncatedTo(ChronoUnit.MINUTES);
        }
        LocalDateTime dataTime = defaultTime;

        for (String segment : cp.split(";")) {
            for (String token : segment.split(",")) {
                String t = token.trim();
                if (t.isEmpty()) {
                    continue;
                }
                int eq = t.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = t.substring(0, eq).trim();
                String value = t.substring(eq + 1).trim();
                if ("DataTime".equalsIgnoreCase(key)) {
                    LocalDateTime parsed = Hj212DataParser.parseDataTime(value);
                    if (parsed != null) {
                        dataTime = parsed;
                    }
                    continue;
                }
                String code = canonical(key);
                if (code == null || dataTime == null) {
                    continue;
                }
                WorkParamPoint point = new WorkParamPoint();
                point.setParamCode(code);
                point.setParamValue(value);
                point.setCollectedAt(dataTime);
                out.add(point);
            }
        }
        return out;
    }
}
