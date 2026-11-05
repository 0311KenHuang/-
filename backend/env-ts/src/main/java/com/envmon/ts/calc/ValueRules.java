package com.envmon.ts.calc;

import com.envmon.ts.calc.TsProperties.NegativeFix;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 附录 G 的计算前置处理：负值修约 + 超量程夹取。
 *
 * 两条规则都只改"参与统计的值"，不改原始报文——原文永远保留在 hj212_frame，
 * 任何修正都能被追溯和推翻。
 */
public final class ValueRules {

    private ValueRules() {
    }

    /**
     * @param factorCode  因子编码
     * @param value       上报值，可为空
     * @param devFlag     设备标记，可为空
     * @param rangeMax    该因子量程上限，未知传 null
     * @param negativeFix 负值修约规则，按因子配置
     */
    public static Result apply(String factorCode, BigDecimal value, String devFlag,
                               BigDecimal rangeMax, Map<String, NegativeFix> negativeFix) {
        Result result = new Result(value, devFlag);
        if (value == null) {
            return result;
        }

        // 负值修约：如 SO₂ 标干浓度在 (-15, 0) 区间时按 0.5 计，避免负值把均值拉低
        NegativeFix fix = negativeFix == null ? null : negativeFix.get(factorCode);
        if (fix != null && value.signum() < 0) {
            BigDecimal lower = fix.getLowerBound() == null ? new BigDecimal("-15") : fix.getLowerBound();
            if (value.compareTo(lower) > 0) {
                result.setValue(fix.getTarget() == null ? new BigDecimal("0.5") : fix.getTarget());
            }
        }

        // 超量程：以量程上限参与统计，并补 T 标记（设备自己标了 D/C/M 的保持原标记）
        BigDecimal current = result.getValue();
        if (rangeMax != null && current.compareTo(rangeMax) > 0) {
            result.setValue(rangeMax);
            String flag = result.getDevFlag();
            if (flag == null || flag.trim().isEmpty() || "N".equalsIgnoreCase(flag)) {
                result.setDevFlag("T");
            }
        }
        return result;
    }

    @Data
    public static class Result {
        private BigDecimal value;
        private String devFlag;

        public Result(BigDecimal value, String devFlag) {
            this.value = value;
            this.devFlag = devFlag;
        }
    }
}
