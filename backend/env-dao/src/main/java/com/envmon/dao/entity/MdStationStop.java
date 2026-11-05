package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站房停运区间。
 *
 * 不继承 TenantEntity：没有软删除字段，且聚合任务在无租户上下文的后台线程里
 * 也要读它（表已加入租户插件忽略名单），tenant_id 由状态切换时显式写入。
 */
@Data
@TableName("md_station_stop")
public class MdStationStop implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long stationId;

    private LocalDateTime beginAt;

    /** 为空表示仍在停运 */
    private LocalDateTime endAt;

    private String reason;

    /** STATUS / MANUAL / IMPORT */
    private String source;
}
