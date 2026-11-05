package com.envmon.service.alarm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 报警扫描窗口配置，参数按现场数据频次调整 */
@Data
@Component
@ConfigurationProperties(prefix = "alarm.eval")
public class AlarmProperties {

    /** 超标判定回看窗口：要覆盖"连续 3 分钟"，留足扫描间隔的余量 */
    private int overLimitWindowMinutes = 10;

    /** 恒值判定回看窗口：至少容纳 10 条分钟数据 */
    private int deadLookbackMinutes = 30;

    /** 缺数判定回看天数：超过这个窗口没有数据的老因子不再新开报警，避免扫全表 */
    private int missingLookbackDays = 2;

    /** 抑制窗口查询上限（秒），覆盖规则里最长的 suppress_sec */
    private int suppressLookbackSeconds = 21600;
}
