package com.envmon.ts.parse;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CP 数据段解析：把横向的 {因子}-Rtd / {因子}-Flag 拆成纵向分钟记录。
 *
 * 覆盖的报文：
 *   2011 实时数据、2051 分钟数据、2041 分钟历史（补传）
 * 2061/2031 是设备侧算好的小时/日数据，不在这里落分钟表——
 * 平台的小时/日均值统一由分钟数据按附录 G 重算，避免两套口径打架。
 *
 * 解析约定：
 *   - DataTime 是分组的边界，补传报文一个 CP 可含多组；
 *   - 时间取 DataTime，缺失时退回 QN 前 14 位；
 *   - 数值非法（空、--、N/A）置 null，但保留该分钟，口径上算无效数据；
 *   - 设备标记可能多值（如 "N,D"），按 D &gt; C &gt; M &gt; T &gt; Td 取最严重的一个。
 */
@Slf4j
public final class Hj212DataParser {

    /** 落到分钟表的命令编码 */
    private static final Set<String> MINUTE_CN = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("2011", "2051", "2041")));

    /** 设备标记优先级：越靠前越严重 */
    private static final String[] FLAG_PRIORITY = {"D", "C", "M", "T", "Td"};

    private static final DateTimeFormatter FMT_14 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter FMT_17 = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final DateTimeFormatter FMT_12 = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final DateTimeFormatter FMT_10 = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private Hj212DataParser() {
    }

    public static boolean isMinuteData(String cn) {
        return cn != null && MINUTE_CN.contains(cn.trim());
    }

    /**
     * 按 CP 原文解析。
     *
     * @param cp           CP 原文，不含 &&；空则返回空列表
     * @param qn           报文 QN，DataTime 缺失时兜底
     * @param fallbackTime 再兜底（报文接收时间）
     */
    public static List<CpSample> parse(String cp, String qn, LocalDateTime fallbackTime) {
        List<CpSample> out = new ArrayList<CpSample>();
        if (StringUtils.isBlank(cp)) {
            return out;
        }
        LocalDateTime defaultTime = defaultTime(qn, fallbackTime);
        CpSample current = new CpSample();
        current.setDataTime(defaultTime);

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
                String val = t.substring(eq + 1).trim();

                if ("DataTime".equalsIgnoreCase(key)) {
                    // 新的一组数据开始：上一组有因子就落盘
                    if (!current.isEmpty()) {
                        out.add(current);
                        current = new CpSample();
                    }
                    LocalDateTime parsed = parseDataTime(val);
                    if (parsed != null) {
                        current.setDataTime(parsed);
                    } else {
                        current.setDataTime(defaultTime);
                    }
                    continue;
                }
                applyToken(key, val, current);
            }
        }
        if (!current.isEmpty()) {
            out.add(current);
        }
        // 没有时间的数据无法落到时间轴，宁可丢弃并留日志
        List<CpSample> valid = new ArrayList<CpSample>(out.size());
        for (CpSample s : out) {
            if (s.getDataTime() == null) {
                log.warn("CP 缺少 DataTime 且 QN 不可解析，丢弃该组因子: {}", s.getFactors().keySet());
            } else {
                valid.add(s);
            }
        }
        return valid;
    }

    /**
     * 按已解析的 params 键值对解析（兼容历史报文）。
     * 注意：JSON 对象会折叠重复 key，多 DataTime 的补传报文应优先走 CP 原文。
     */
    public static List<CpSample> parseParams(Map<String, String> params, String qn, LocalDateTime fallbackTime) {
        List<CpSample> out = new ArrayList<CpSample>();
        if (params == null || params.isEmpty()) {
            return out;
        }
        CpSample sample = new CpSample();
        LocalDateTime time = parseDataTime(params.get("DataTime"));
        sample.setDataTime(time != null ? time : defaultTime(qn, fallbackTime));
        for (Map.Entry<String, String> e : params.entrySet()) {
            applyToken(e.getKey(), e.getValue(), sample);
        }
        if (!sample.isEmpty() && sample.getDataTime() != null) {
            out.add(sample);
        }
        return out;
    }

    private static void applyToken(String key, String val, CpSample sample) {
        int dash = key.lastIndexOf('-');
        if (dash <= 0 || dash == key.length() - 1) {
            return;
        }
        String code = key.substring(0, dash).trim();
        String suffix = key.substring(dash + 1).trim();
        if (code.isEmpty()) {
            return;
        }
        if ("Rtd".equalsIgnoreCase(suffix)) {
            sample.reading(code).setValue(parseValue(val));
        } else if ("Flag".equalsIgnoreCase(suffix)) {
            sample.reading(code).setDevFlag(normalizeFlag(val));
        }
        // ZsRtd（折算值）/ ZsFlag：暂不落库，等超标判定与排放量计算时再启用，
        // 避免现在就用一个没校准过的折算口径污染历史数据。
    }

    /** 数值解析：设备偶尔上报空、"--"、N/A，统一置 null 并计入无效 */
    static BigDecimal parseValue(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        if (v.isEmpty() || "--".equals(v) || "null".equalsIgnoreCase(v)
                || "n/a".equalsIgnoreCase(v) || "nan".equalsIgnoreCase(v)) {
            return null;
        }
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1).trim();
        }
        try {
            return new BigDecimal(v);
        } catch (NumberFormatException e) {
            log.debug("数值非法，按空值处理: {}", raw);
            return null;
        }
    }

    /**
     * 标记规范化：多值时按 优先级 D &gt; C &gt; M &gt; T &gt; Td 取最严重；
     * 未知标记原样保留（大写，最多 4 位），不猜含义。
     */
    static String normalizeFlag(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String best = null;
        int bestRank = -1;
        for (String part : raw.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            String canonical = canonicalFlag(p);
            int rank = rank(canonical != null ? canonical : p);
            if (rank > bestRank) {
                bestRank = rank;
                best = canonical != null ? canonical : upper(p);
            }
        }
        return best;
    }

    private static String canonicalFlag(String s) {
        if ("N".equalsIgnoreCase(s)) {
            return "N";
        }
        for (String f : FLAG_PRIORITY) {
            if (f.equalsIgnoreCase(s)) {
                return f;
            }
        }
        return null;
    }

    private static int rank(String flag) {
        if (flag == null) {
            return -1;
        }
        if ("N".equalsIgnoreCase(flag)) {
            return 0;
        }
        for (int i = 0; i < FLAG_PRIORITY.length; i++) {
            if (FLAG_PRIORITY[i].equalsIgnoreCase(flag)) {
                // D 最严重，rank 最大
                return FLAG_PRIORITY.length - i;
            }
        }
        return 1;
    }

    private static String upper(String s) {
        String u = s.toUpperCase();
        return u.length() > 4 ? u.substring(0, 4) : u;
    }

    static LocalDateTime parseDataTime(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String v = raw.trim();
        try {
            if (v.matches("\\d{17}")) {
                return LocalDateTime.parse(v, FMT_17).truncatedTo(ChronoUnit.MINUTES);
            }
            if (v.matches("\\d{14}")) {
                return LocalDateTime.parse(v, FMT_14).truncatedTo(ChronoUnit.MINUTES);
            }
            if (v.matches("\\d{12}")) {
                return LocalDateTime.parse(v, FMT_12).truncatedTo(ChronoUnit.MINUTES);
            }
            if (v.matches("\\d{10}")) {
                return LocalDateTime.parse(v, FMT_10).truncatedTo(ChronoUnit.MINUTES);
            }
        } catch (DateTimeParseException e) {
            log.debug("DataTime 非法: {}", raw);
        }
        return null;
    }

    /** QN：yyyyMMddHHmmssSSS + 3 位序号，前 14 位就是数据时间 */
    static LocalDateTime timeFromQn(String qn) {
        if (qn == null || qn.length() < 14) {
            return null;
        }
        String head = qn.substring(0, 14);
        if (!head.matches("\\d{14}")) {
            return null;
        }
        return parseDataTime(head);
    }

    private static LocalDateTime defaultTime(String qn, LocalDateTime fallbackTime) {
        LocalDateTime t = timeFromQn(qn);
        if (t != null) {
            return t;
        }
        return fallbackTime == null ? null : fallbackTime.truncatedTo(ChronoUnit.MINUTES);
    }
}
