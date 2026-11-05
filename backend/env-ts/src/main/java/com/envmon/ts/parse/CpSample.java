package com.envmon.ts.parse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一个 DataTime 下的全部因子读数。
 *
 * 补传报文（CN=2041）会在一个 CP 里带多组 DataTime，
 * 必须按时间分组落库，不能把所有因子塞进同一个时刻。
 */
@Data
public class CpSample {

    private LocalDateTime dataTime;

    private final Map<String, FactorReading> factors = new LinkedHashMap<String, FactorReading>();

    public boolean isEmpty() {
        return factors.isEmpty();
    }

    public FactorReading reading(String factorCode) {
        FactorReading r = factors.get(factorCode);
        if (r == null) {
            r = new FactorReading();
            factors.put(factorCode, r);
        }
        return r;
    }
}
