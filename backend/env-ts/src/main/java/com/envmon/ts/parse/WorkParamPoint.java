package com.envmon.ts.parse;

import lombok.Data;

import java.time.LocalDateTime;

/** 一条工作参数读数 */
@Data
public class WorkParamPoint {

    /** 归一化参数编码；带因子前缀时为 "因子:参数" */
    private String paramCode;
    private String paramValue;
    private LocalDateTime collectedAt;
}
