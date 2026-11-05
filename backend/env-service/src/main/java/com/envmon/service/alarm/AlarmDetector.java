package com.envmon.service.alarm;

import com.envmon.dao.dto.AlarmSampleRow;
import com.envmon.dao.dto.LastPointRow;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 规则判定：纯函数，不碰数据库，便于单测覆盖边界。
 *
 * 三条规则都只回答"当前窗口里有没有命中"，事件的去重/抑制/关闭由引擎负责。
 */
public final class AlarmDetector {

    private AlarmDetector() {
    }

    public static String key(Long stationId, String factorCode) {
        return stationId + "|" + (factorCode == null ? "" : factorCode);
    }

    /**
     * 超标：连续 N 分钟都超过限值。
     *
     * 入参只包含"有效且超限"的分钟，所以时间断档（缺数或回到限值内）即中断连续计数。
     */
    public static List<AlarmHit> overLimit(List<AlarmSampleRow> rows, int consecutive) {
        if (rows == null || rows.isEmpty() || consecutive <= 0) {
            return Collections.emptyList();
        }
        List<AlarmHit> hits = new ArrayList<AlarmHit>();
        for (List<AlarmSampleRow> series : group(rows).values()) {
            sortByTs(series);
            int run = 0;
            LocalDateTime runStart = null;
            AlarmSampleRow prev = null;
            for (AlarmSampleRow row : series) {
                boolean continuous = prev != null
                        && prev.getTs() != null && row.getTs() != null
                        && row.getTs().equals(prev.getTs().plusMinutes(1));
                if (!continuous) {
                    run = 1;
                    runStart = row.getTs();
                } else {
                    run++;
                }
                prev = row;
                if (run >= consecutive) {
                    hits.add(hit(row, runStart));
                    break;
                }
            }
        }
        return hits;
    }

    /**
     * 恒值：最近 N 条有效数值完全相等（方差为 0）。
     * 只看最近 N 条，不要求分钟严格连续——设备上报间隔不固定时也要能识别数据保持。
     */
    public static List<AlarmHit> deadValue(List<AlarmSampleRow> points, int consecutive) {
        if (points == null || points.isEmpty() || consecutive <= 0) {
            return Collections.emptyList();
        }
        List<AlarmHit> hits = new ArrayList<AlarmHit>();
        for (List<AlarmSampleRow> series : group(points).values()) {
            if (series.size() < consecutive) {
                continue;
            }
            sortByTs(series);
            List<AlarmSampleRow> tail = series.subList(series.size() - consecutive, series.size());
            BigDecimal first = tail.get(0).getValue();
            if (first == null) {
                continue;
            }
            boolean allEqual = true;
            for (AlarmSampleRow row : tail) {
                if (row.getValue() == null || row.getValue().compareTo(first) != 0) {
                    allEqual = false;
                    break;
                }
            }
            if (allEqual) {
                AlarmHit hit = hit(tail.get(tail.size() - 1), tail.get(0).getTs());
                hits.add(hit);
            }
        }
        return hits;
    }

    /**
     * 缺数：某站某因子最后一次上报距今超过 gap 分钟。
     *
     * 同时返回 freshKeys（数据仍然新鲜的 key）：
     * 只有明确恢复上报的才能自动关闭缺数事件，避免因回看窗口取不到数据而误关。
     */
    public static MissingResult missing(List<LastPointRow> lastPoints, LocalDateTime now,
                                        int gapMinutes, Set<Long> skippedStations) {
        MissingResult result = new MissingResult();
        if (lastPoints == null || lastPoints.isEmpty()) {
            return result;
        }
        Set<Long> skipped = skippedStations == null ? Collections.<Long>emptySet() : skippedStations;
        for (LastPointRow row : lastPoints) {
            if (row.getLastTs() == null || row.getStationId() == null) {
                continue;
            }
            String key = key(row.getStationId(), row.getFactorCode());
            if (skipped.contains(row.getStationId())) {
                // 停运站房本来就不该有数据，算"无需报警"，同时允许关闭遗留的缺数事件
                result.getFreshKeys().add(key);
                continue;
            }
            long gap = Duration.between(row.getLastTs(), now).toMinutes();
            if (gap > gapMinutes) {
                AlarmHit hit = new AlarmHit();
                hit.setStationId(row.getStationId());
                hit.setFactorCode(row.getFactorCode());
                hit.setBeginAt(row.getLastTs().plusMinutes(1));
                result.getHits().add(hit);
            } else {
                result.getFreshKeys().add(key);
            }
        }
        return result;
    }

    private static AlarmHit hit(AlarmSampleRow row, LocalDateTime beginAt) {
        AlarmHit hit = new AlarmHit();
        hit.setStationId(row.getStationId());
        hit.setFactorCode(row.getFactorCode());
        hit.setValue(row.getValue());
        hit.setThreshold(row.getThreshold());
        hit.setBeginAt(beginAt == null ? row.getTs() : beginAt);
        return hit;
    }

    private static Map<String, List<AlarmSampleRow>> group(List<AlarmSampleRow> rows) {
        Map<String, List<AlarmSampleRow>> map = new LinkedHashMap<String, List<AlarmSampleRow>>();
        for (AlarmSampleRow row : rows) {
            if (row.getStationId() == null) {
                continue;
            }
            String key = key(row.getStationId(), row.getFactorCode());
            List<AlarmSampleRow> list = map.get(key);
            if (list == null) {
                list = new ArrayList<AlarmSampleRow>();
                map.put(key, list);
            }
            list.add(row);
        }
        return map;
    }

    private static void sortByTs(List<AlarmSampleRow> series) {
        Collections.sort(series, new Comparator<AlarmSampleRow>() {
            @Override
            public int compare(AlarmSampleRow a, AlarmSampleRow b) {
                if (a.getTs() == null || b.getTs() == null) {
                    return 0;
                }
                return a.getTs().compareTo(b.getTs());
            }
        });
    }

    @Data
    public static class MissingResult {
        private List<AlarmHit> hits = new ArrayList<AlarmHit>();
        private Set<String> freshKeys = new HashSet<String>();
    }
}
