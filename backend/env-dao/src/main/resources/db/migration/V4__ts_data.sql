-- ============================================================
-- V4 时序数据域：分钟 / 小时 / 日（附录 G）
--
-- TDengine 服务端尚未提供，先用 MySQL 承载。字段与 docs/02 §5.1 对齐，
-- 切 TDengine 时只替换 Mapper 的写入/聚合 SQL，Service 与接口不变。
--
-- 分钟表唯一键 (station_id, factor_code, ts)：
--   设备重发、断网补传都会重复上报同一分钟，靠唯一键幂等覆盖，
--   保证"重复报文只更新最后一次"，不会把平均值算歪。
-- ============================================================

CREATE TABLE IF NOT EXISTS ts_data_minute (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT        NOT NULL,
  station_id  BIGINT        NOT NULL COMMENT '站房',
  factor_code VARCHAR(16)   NOT NULL COMMENT 'HJ212 参数编码，如 a21026',
  ts          DATETIME      NOT NULL COMMENT '数据时间（DataTime，截断到分钟）',
  `value`     DECIMAL(18,6) NULL     COMMENT '实测值 {因子}-Rtd',
  raw_value   DECIMAL(18,6) NULL     COMMENT '原始信号值（预留，暂未采集）',
  dev_flag    VARCHAR(4)    NULL     COMMENT '设备标记 N/D/C/M/T/Td',
  prod_flag   VARCHAR(4)    NULL     COMMENT '生产工况标记 N/St/Sd/B/F（预留）',
  quality     TINYINT       NOT NULL DEFAULT 3 COMMENT '1有效 2无效 3未标记',
  source_cn   VARCHAR(8)    NULL     COMMENT '来源命令编码 2011/2051/2041',
  frame_id    BIGINT        NULL     COMMENT 'hj212_frame.id，争议时回溯原文',
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_minute_point (station_id, factor_code, ts),
  KEY idx_minute_ts (ts),
  KEY idx_minute_station_ts (station_id, ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分钟数据（TDengine 上线前的落地表）';

CREATE TABLE IF NOT EXISTS ts_data_hour (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT        NOT NULL,
  station_id  BIGINT        NOT NULL,
  factor_code VARCHAR(16)   NOT NULL,
  ts          DATETIME      NOT NULL COMMENT '小时起点，如 2026-11-04 14:00:00',
  avg_value   DECIMAL(18,6) NULL     COMMENT '有效分钟均值',
  max_value   DECIMAL(18,6) NULL,
  min_value   DECIMAL(18,6) NULL,
  valid_cnt   INT           NOT NULL DEFAULT 0 COMMENT '有效分钟数（quality=1）',
  total_cnt   INT           NOT NULL DEFAULT 0 COMMENT '实际收到分钟数',
  valid_rate  DECIMAL(6,4)  NOT NULL DEFAULT 0 COMMENT 'valid_cnt / 60，附录 G 口径',
  is_valid    TINYINT       NOT NULL DEFAULT 0 COMMENT '1 有效：valid_cnt >= 45',
  flag        VARCHAR(4)    NULL     COMMENT '有效=N；无效取最严重标记；单纯缺数为 U',
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_hour_point (station_id, factor_code, ts),
  KEY idx_hour_ts (ts),
  KEY idx_hour_station_ts (station_id, ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小时数据（附录 G：≥45 有效分钟）';

CREATE TABLE IF NOT EXISTS ts_data_day (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  tenant_id       BIGINT        NOT NULL,
  station_id      BIGINT        NOT NULL,
  factor_code     VARCHAR(16)   NOT NULL,
  ts              DATETIME      NOT NULL COMMENT '日起点 00:00:00',
  avg_value       DECIMAL(18,6) NULL     COMMENT '有效小时均值',
  max_value       DECIMAL(18,6) NULL,
  min_value       DECIMAL(18,6) NULL,
  emission        DECIMAL(18,6) NULL     COMMENT '日排放量（预留）',
  valid_hour_cnt  INT           NOT NULL DEFAULT 0,
  total_hour_cnt  INT           NOT NULL DEFAULT 0,
  valid_rate      DECIMAL(6,4)  NOT NULL DEFAULT 0 COMMENT '有效小时数 / 24',
  is_valid        TINYINT       NOT NULL DEFAULT 0 COMMENT '1 有效：有效小时数 >= 18（75%）',
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_day_point (station_id, factor_code, ts),
  KEY idx_day_ts (ts),
  KEY idx_day_station_ts (station_id, ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日数据（附录 G：≥75% 有效小时）';

-- 补存 CP 原文：重算分钟数据时按原样解析。
-- 只存 params JSON 是不够的——补传报文里可能有多个 DataTime 组，
-- JSON 对象会把重复 key 折叠掉，历史分钟会丢。
ALTER TABLE hj212_frame
  ADD COLUMN cp TEXT NULL COMMENT 'CP 原文（不含 &&），重算时按原样解析' AFTER params;
