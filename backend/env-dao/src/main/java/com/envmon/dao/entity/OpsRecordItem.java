package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/** 运维记录明细：检查项结果 */
@Data
@TableName("ops_record_item")
public class OpsRecordItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long recordId;
    private String itemCode;
    private String itemName;
    /** NUMBER / TEXT / SELECT / PHOTO / SIGN */
    private String valueType;
    private String value;
    private String unit;
    /** 1 正常 / 0 异常 */
    private Integer normalFlag;
}
