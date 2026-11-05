package com.envmon.service.alarm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** 规则参数：从 alm_rule.params_json 反序列化，缺省值由调用方兜底 */
@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlarmParams {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 超标/恒值：需要连续满足的条数 */
    private Integer consecutive;

    /** 缺数：连续缺失分钟数 */
    private Integer gapMin;

    public static AlarmParams parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new AlarmParams();
        }
        try {
            return MAPPER.readValue(json, AlarmParams.class);
        } catch (Exception e) {
            log.warn("规则参数解析失败，按默认值处理: {}", json);
            return new AlarmParams();
        }
    }
}
