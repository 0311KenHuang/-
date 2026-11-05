package com.envmon.service.ops;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 运维模块请求入参（静态内部类，避免一堆碎文件） */
public final class OpsRequest {

    private OpsRequest() {
    }

    @Data
    public static class PlanGenerate implements Serializable {
        private static final long serialVersionUID = 1L;
        /** YYYY-MM，留空默认当月（Service 兜底） */
        @Size(max = 7, message = "period 格式应为 YYYY-MM")
        private String period;
        private List<String> opsTypes = new ArrayList<String>();
        private LocalDate planDate;
        private Long assigneeId;
    }

    @Data
    public static class TaskQuery implements Serializable {
        private static final long serialVersionUID = 1L;
        private int page = 1;
        private int pageSize = 20;
        private String status;
        private Long stationId;
        private Long assigneeId;
        private String source;
    }

    @Data
    public static class TaskAction implements Serializable {
        private static final long serialVersionUID = 1L;
        /** ACCEPT / ARRIVE / SUBMIT / TRANSFER / RETURN，必填 */
        @NotBlank(message = "流转动作不能为空")
        @Size(max = 32)
        private String action;
        @Size(max = 500, message = "备注最长 500 字")
        private String remark;
        /** 到场打卡坐标 */
        private Double lng;
        private Double lat;
        @Size(max = 256)
        private String photoKey;
        /** 改派目标 */
        private Long assigneeId;
    }

    @Data
    public static class RecordSubmit implements Serializable {
        private static final long serialVersionUID = 1L;
        @NotNull(message = "taskId 不能为空")
        private Long taskId;
        private LocalDateTime beginTime;
        private LocalDateTime endTime;
        private LocalDateTime markBegin;
        private LocalDateTime markEnd;
        @Size(max = 2000, message = "结论最长 2000 字")
        private String conclusion;
        private LocalDate nextPlanHint;
        @Valid
        private List<Item> items = new ArrayList<Item>();
        @Valid
        private List<Media> media = new ArrayList<Media>();
    }

    @Data
    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;
        @NotBlank(message = "itemCode 不能为空")
        @Size(max = 64)
        private String itemCode;
        @Size(max = 128)
        private String itemName;
        @Size(max = 32)
        private String valueType;
        @Size(max = 256)
        private String value;
        @Size(max = 32)
        private String unit;
        private Integer normalFlag;
    }

    @Data
    public static class Media implements Serializable {
        private static final long serialVersionUID = 1L;
        @Size(max = 32)
        private String mediaType;
        @Size(max = 256)
        private String objectKey;
        private String watermarkJson;
        @Size(max = 64)
        private String sha256;
    }

    @Data
    public static class Audit implements Serializable {
        private static final long serialVersionUID = 1L;
        /** PASS / REJECT */
        @NotBlank(message = "审核结果不能为空")
        @Pattern(regexp = "PASS|REJECT", message = "审核结果只能是 PASS 或 REJECT")
        private String result;
        @Size(max = 500, message = "审核意见最长 500 字")
        private String reason;
    }
}
