package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/** 现场照片/视频：水印信息与摘要留痕 */
@Data
@TableName("ops_record_media")
public class OpsRecordMedia implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long recordId;
    /** PHOTO / VIDEO / FILE */
    private String mediaType;
    private String objectKey;
    private String watermarkJson;
    private String sha256;
}
