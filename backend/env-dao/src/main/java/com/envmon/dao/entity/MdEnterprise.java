package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_enterprise")
public class MdEnterprise extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 统一社会信用代码 */
    private String creditCode;
    private String name;
    /** 简称，用于站房编码 */
    private String shortName;
    private String industryCode;
    /** 排污许可证号 */
    private String permitNo;
    private Long regionId;
    private String address;
    private String lng;
    private String lat;
    /** 是否重点排污单位 */
    private Integer isKeyUnit;
    private Integer status;
}
