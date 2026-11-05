-- ============================================================
-- V9 工作参数监控（docs/03 §4「上传工作参数」）
--
-- 2025 新增上传内容：量程、斜率、截距、校准浓度、消解温度。
-- 这些参数是「穿透式监管」的关键证据：参数被改过却没有留痕，
-- 后面所有的浓度数据都不可信。快照只追加，变更单独记一条。
-- ============================================================

CREATE TABLE IF NOT EXISTS dev_param_snapshot (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id    BIGINT       NOT NULL,
  station_id   BIGINT       NOT NULL,
  device_id    BIGINT       NULL,
  mn           VARCHAR(64)  NULL,
  param_code   VARCHAR(32)  NOT NULL COMMENT 'RANGE_MIN/RANGE_MAX/SLOPE/INTERCEPT/CAL_CONC/DIGEST_TEMP/SN/CPUID/MAC/FIRMWARE',
  param_value  VARCHAR(128) NULL,
  collected_at DATETIME     NOT NULL,
  source       VARCHAR(16)  NOT NULL DEFAULT 'DTU',
  frame_id     BIGINT       NULL COMMENT '来源报文，可回溯原文',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_snapshot_device (device_id, param_code, collected_at),
  KEY idx_snapshot_station (station_id, collected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作参数快照（只追加）';

CREATE TABLE IF NOT EXISTS dev_param_change (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id    BIGINT       NOT NULL,
  station_id   BIGINT       NOT NULL,
  device_id    BIGINT       NULL,
  param_code   VARCHAR(32)  NOT NULL,
  old_value    VARCHAR(128) NULL,
  new_value    VARCHAR(128) NULL,
  change_time  DATETIME     NOT NULL,
  operator     BIGINT       NULL,
  evidence_key VARCHAR(255) NULL COMMENT '截图/日志文件',
  frame_id     BIGINT       NULL,
  event_id     BIGINT       NULL COMMENT '关联报警事件',
  PRIMARY KEY (id),
  KEY idx_change_device (device_id, change_time),
  KEY idx_change_station (station_id, change_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数变更留痕';
