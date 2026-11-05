package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报警规则。
 *
 * 本期只内置三条数据类规则，求值逻辑按 rule_code 走代码；
 * params_json 存阈值参数（连续条数、缺失分钟数），改规则不用改代码。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alm_rule")
public class AlmRule extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** DATA_OVER_LIMIT / DATA_DEAD_VALUE / DATA_MISSING */
    private String ruleCode;
    private String name;
    /** DATA / VALIDITY / DEVICE / OPS / SECURITY */
    private String category;
    /** P1 / P2 / P3 */
    private String level;
    /** 通用表达式，内置规则暂时按 rule_code 求值 */
    private String expression;
    /** 条件需持续秒数 */
    private Integer durationSec;
    /** 同对象同规则抑制窗口 */
    private Integer suppressSec;
    /** 规则参数 JSON，如 {"consecutive":3} */
    private String paramsJson;
    private String windowJson;
    private String channels;
    private String escalateJson;
    private Integer enabled;
}
