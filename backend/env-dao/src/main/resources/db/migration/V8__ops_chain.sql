-- ============================================================
-- V8 运维证据链：计划 → 任务 → 现场记录 → 审核
--
-- 对应 docs/01 §4 六环节与 §6 的 BR-01~06：
--   该做的（计划）/ 派了人（任务）/ 到场了（打卡）/ 真干了（记录明细）
--   / 设备状态（数据标记）/ 有人负责（审核 + 流转日志）。
-- 所有业务表带 tenant_id；ops_task 为 V1 已有表，这里补两个字段。
-- ============================================================

CREATE TABLE IF NOT EXISTS ops_plan (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id    BIGINT      NOT NULL,
  plan_no      VARCHAR(32) NOT NULL,
  station_id   BIGINT      NOT NULL,
  period       CHAR(7)     NOT NULL COMMENT 'YYYY-MM',
  plan_type    VARCHAR(24) NOT NULL,
  planned_date DATE        NOT NULL,
  source       VARCHAR(8)  NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO/MANUAL',
  status       VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/APPROVED/DONE/MISSED',
  approve_by   BIGINT      NULL,
  approve_at   DATETIME    NULL,
  created_by   BIGINT      NULL,
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_plan_no (plan_no),
  KEY idx_plan_station_period (station_id, period),
  KEY idx_plan_period_status (period, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维计划（月）';

CREATE TABLE IF NOT EXISTS ops_plan_item (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT      NOT NULL,
  plan_id     BIGINT      NOT NULL,
  ops_type    VARCHAR(24) NOT NULL COMMENT '巡检/校准/校验/标液核查/试剂更换/故障维修',
  plan_date   DATE        NOT NULL,
  assignee_id BIGINT      NULL,
  done        TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_plan_item_plan (plan_id),
  KEY idx_plan_item_assignee (assignee_id, plan_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划明细：一条明细实例化为一个任务';

CREATE TABLE IF NOT EXISTS ops_record (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id      BIGINT       NOT NULL,
  record_no      VARCHAR(32)  NOT NULL,
  task_id        BIGINT       NOT NULL,
  station_id     BIGINT       NOT NULL,
  ops_type       VARCHAR(24)  NOT NULL,
  engineer_id    BIGINT       NOT NULL,
  begin_time     DATETIME     NOT NULL,
  end_time       DATETIME     NULL,
  mark_begin     DATETIME     NULL COMMENT '应产生的数据标记起点',
  mark_end       DATETIME     NULL COMMENT '应产生的数据标记终点',
  conclusion     VARCHAR(500) NULL,
  next_plan_hint DATE         NULL,
  status         VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/APPROVED/REJECTED',
  sign_engineer  VARCHAR(255) NULL,
  sign_auditor   VARCHAR(255) NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_record_no (record_no),
  UNIQUE KEY uk_record_task (task_id),
  KEY idx_record_station_time (station_id, ops_type, begin_time),
  KEY idx_record_status (status, begin_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维记录';

CREATE TABLE IF NOT EXISTS ops_record_item (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT       NOT NULL,
  record_id   BIGINT       NOT NULL,
  item_code   VARCHAR(32)  NOT NULL,
  item_name   VARCHAR(128) NOT NULL,
  value_type  VARCHAR(16)  NOT NULL COMMENT 'NUMBER/TEXT/SELECT/PHOTO/SIGN',
  `value`     VARCHAR(1000) NULL,
  unit        VARCHAR(16)  NULL,
  normal_flag TINYINT      NULL COMMENT '1正常 0异常',
  PRIMARY KEY (id),
  KEY idx_record_item (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维记录明细';

CREATE TABLE IF NOT EXISTS ops_record_media (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id      BIGINT       NOT NULL,
  record_id      BIGINT       NOT NULL,
  media_type     VARCHAR(8)   NOT NULL COMMENT 'PHOTO/VIDEO/FILE',
  object_key     VARCHAR(255) NOT NULL,
  watermark_json JSON         NULL COMMENT '{time,lng,lat,station,user}',
  sha256         CHAR(64)     NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_record_media (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维多媒体（水印与摘要）';

CREATE TABLE IF NOT EXISTS ops_checkin (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT        NOT NULL,
  task_id     BIGINT        NOT NULL,
  user_id     BIGINT        NOT NULL,
  station_id  BIGINT        NOT NULL,
  lng         DECIMAL(10,7) NULL,
  lat         DECIMAL(10,7) NULL,
  distance_m  INT           NULL COMMENT '与站房距离(米)',
  photo_key   VARCHAR(255)  NULL,
  in_at       DATETIME      NULL,
  out_at      DATETIME      NULL,
  is_valid    TINYINT       NOT NULL DEFAULT 1 COMMENT '距离<=300m 才计为有效到场',
  PRIMARY KEY (id),
  KEY idx_checkin_task (task_id),
  KEY idx_checkin_user_time (user_id, in_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='现场打卡';

CREATE TABLE IF NOT EXISTS ops_audit (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id  BIGINT       NOT NULL,
  record_id  BIGINT       NOT NULL,
  auditor_id BIGINT       NOT NULL,
  result     VARCHAR(8)   NOT NULL COMMENT 'PASS/REJECT',
  reason     VARCHAR(500) NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_audit_record (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维审核（只追加）';

CREATE TABLE IF NOT EXISTS ops_task_log (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id  BIGINT       NOT NULL,
  task_id    BIGINT       NOT NULL,
  action     VARCHAR(16)  NOT NULL COMMENT 'DISPATCH/ACCEPT/ARRIVE/SUBMIT/TRANSFER/RETURN/CLOSE',
  operator   BIGINT       NOT NULL,
  remark     VARCHAR(255) NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_task_log (task_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务流转日志（只追加）';

ALTER TABLE ops_task
  ADD COLUMN plan_item_id BIGINT NULL COMMENT '来源计划明细；故障/报警任务为空' AFTER task_no;

ALTER TABLE ops_task
  ADD COLUMN sla_json JSON NULL COMMENT 'SLA 快照' AFTER deadline;
