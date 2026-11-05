package com.envmon.service.ops;

import com.envmon.dao.entity.OpsAudit;
import com.envmon.dao.entity.OpsRecord;
import com.envmon.dao.entity.OpsRecordItem;
import com.envmon.dao.entity.OpsRecordMedia;
import com.envmon.dao.entity.OpsTask;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 运维模块返回结构 */
public final class OpsView {

    private OpsView() {
    }

    @Data
    public static class TaskView implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String taskNo;
        private Long stationId;
        private String stationName;
        private String opsType;
        private String source;
        private Long assigneeId;
        private String assigneeName;
        private LocalDateTime dispatchAt;
        private LocalDateTime acceptAt;
        private LocalDateTime arriveAt;
        private LocalDateTime finishAt;
        private LocalDateTime deadline;
        private String status;
        private Integer isOverdue;
    }

    @Data
    public static class RecordDetail implements Serializable {
        private static final long serialVersionUID = 1L;
        private OpsRecord record;
        private OpsTask task;
        private List<OpsRecordItem> items = new ArrayList<OpsRecordItem>();
        private List<OpsRecordMedia> media = new ArrayList<OpsRecordMedia>();
        private List<OpsAudit> audits = new ArrayList<OpsAudit>();
    }

    @Data
    public static class Coverage implements Serializable {
        private static final long serialVersionUID = 1L;
        private String period;
        private int totalStations;
        private int plannedStations;
        private double coverageRate;
        private List<Long> unplannedStationIds = new ArrayList<Long>();
    }

    @Data
    public static class TraceResult implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 标记了 C/M/D/Td 但找不到对应运维记录 */
        private List<String> fakeMarks = new ArrayList<String>();
        /** 有校准记录但时段内没有 C 标记 */
        private List<String> missingMarks = new ArrayList<String>();
    }
}
