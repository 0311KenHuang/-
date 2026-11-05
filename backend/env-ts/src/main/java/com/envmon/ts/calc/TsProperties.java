package com.envmon.ts.calc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 附录 G 判定阈值。
 *
 * 这些数字各地监管口径可能微调，所以不写死在代码里，放在配置中：
 *   ts.calc.hour-min-valid-minutes=45
 *   ts.calc.day-valid-rate=0.75
 */
@Data
@Component
@ConfigurationProperties(prefix = "ts.calc")
public class TsProperties {

    /** 一小时的理论分钟数，有效率分母 */
    private int hourMinutes = 60;

    /** 小时值有效所需的最少有效分钟数 */
    private int hourMinValidMinutes = 45;

    /** 一天的理论小时数，日均有效率分母 */
    private int dayHours = 24;

    /** 日均值有效所需的有效小时占比 */
    private BigDecimal dayValidRate = new BigDecimal("0.75");

    /** 实时值回看窗口：设备离线时仍能看到最后一次上报 */
    private int realtimeWindowHours = 24;

    /** 定时任务滚动重算的小时数，用于兜住补传与乱序 */
    private int jobHourWindowHours = 2;

    /**
     * 负值修约规则，按因子配置：
     *   a21002: { lower-bound: -15, target: 0.5 }
     * 含义：值大于 lower-bound 且小于 0 时，按 target 参与统计。
     */
    private Map<String, NegativeFix> negativeFix = new LinkedHashMap<String, NegativeFix>();

    /** 日均有效所需的有效小时数，18 = 24 * 0.75 */
    public int dayRequiredValidHours() {
        return (int) Math.ceil(dayHours * dayValidRate.doubleValue());
    }

    @Data
    public static class NegativeFix {
        private BigDecimal lowerBound = new BigDecimal("-15");
        private BigDecimal target = new BigDecimal("0.5");
    }
}
