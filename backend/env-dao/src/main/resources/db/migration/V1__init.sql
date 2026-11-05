-- ============================================================
-- V1 核心表结构（11 张）
-- 完整 55 张表见 docs/schema.sql，后续按模块增量迁移
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_tenant (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_code VARCHAR(32)  NOT NULL COMMENT '租户编码',
  name        VARCHAR(100) NOT NULL COMMENT '租户名称',
  tenant_type VARCHAR(16)  NOT NULL COMMENT 'REG 监管 / OPS 运维单位 / ENT 企业',
  status      TINYINT      NOT NULL DEFAULT 1,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_code (tenant_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户';

CREATE TABLE IF NOT EXISTS sys_role (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT      NOT NULL,
  role_code   VARCHAR(32) NOT NULL COMMENT 'SUP/REG/ORG/MGR/ENG/ENT',
  name        VARCHAR(64) NOT NULL,
  data_scope  VARCHAR(16) NOT NULL DEFAULT 'ORG' COMMENT 'ALL/REGION/ORG/SELF',
  remark      VARCHAR(255) NULL,
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (tenant_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE IF NOT EXISTS sys_user (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id      BIGINT      NOT NULL,
  username       VARCHAR(64) NOT NULL,
  password_hash  VARCHAR(100) NOT NULL,
  real_name      VARCHAR(64) NOT NULL,
  org_id         BIGINT      NULL,
  phone          VARCHAR(32) NULL,
  cert_no        VARCHAR(64) NULL COMMENT '运维工资质证书号',
  status         TINYINT     NOT NULL DEFAULT 1 COMMENT '1正常 0停用 2锁定',
  last_login_at  DATETIME    NULL,
  created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (tenant_id, username),
  KEY idx_user_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色';

CREATE TABLE IF NOT EXISTS sys_audit_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT       NOT NULL,
  user_id     BIGINT       NOT NULL,
  user_name   VARCHAR(64)  NULL,
  module      VARCHAR(64)  NOT NULL,
  action      VARCHAR(32)  NOT NULL,
  biz_id      VARCHAR(64)  NULL,
  before_json JSON         NULL,
  after_json  JSON         NULL,
  ip          VARCHAR(64)  NULL,
  ua          VARCHAR(255) NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id, created_at),
  KEY idx_audit_user (user_id, created_at),
  KEY idx_audit_action (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志（只追加）';

CREATE TABLE IF NOT EXISTS md_enterprise (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id     BIGINT       NOT NULL,
  credit_code   VARCHAR(32)  NOT NULL COMMENT '统一社会信用代码',
  name          VARCHAR(160) NOT NULL,
  short_name    VARCHAR(32)  NULL COMMENT '简称，用于站房编码',
  industry_code VARCHAR(16)  NULL,
  permit_no     VARCHAR(64)  NULL COMMENT '排污许可证号',
  region_id     BIGINT       NOT NULL,
  address       VARCHAR(255) NULL,
  lng           DECIMAL(10,7) NULL,
  lat           DECIMAL(10,7) NULL,
  is_key_unit   TINYINT      NOT NULL DEFAULT 0,
  status        TINYINT      NOT NULL DEFAULT 1,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_credit_code (tenant_id, credit_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业档案';

CREATE TABLE IF NOT EXISTS md_station (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  station_code    VARCHAR(32)  NOT NULL COMMENT '可读业务编码',
  name            VARCHAR(100) NOT NULL COMMENT '命名规则：企业简称-介质',
  station_type    VARCHAR(20)  NOT NULL,
  enterprise_id   BIGINT       NOT NULL,
  region_id       BIGINT       NOT NULL,
  lng             DECIMAL(10,7) NULL,
  lat             DECIMAL(10,7) NULL,
  address         VARCHAR(255) NULL,
  biz_status      VARCHAR(16)  NOT NULL DEFAULT 'NORMAL',
  stop_reason     VARCHAR(255) NULL,
  ops_org_id      BIGINT       NULL COMMENT '责任运维单位',
  ops_engineer_id BIGINT       NULL,
  is_focus        TINYINT      NOT NULL DEFAULT 0,
  qr_token        VARCHAR(64)  NULL,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_station_code (tenant_id, station_code),
  KEY idx_station_ent (enterprise_id, station_type, biz_status),
  KEY idx_station_org (ops_org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站房';

CREATE TABLE IF NOT EXISTS md_device (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id    BIGINT      NOT NULL,
  device_code  VARCHAR(32) NOT NULL,
  station_id   BIGINT      NOT NULL,
  category     VARCHAR(16) NOT NULL COMMENT 'ANALYZER/DTU/FLOW/OTHER',
  sn           VARCHAR(64) NULL COMMENT '设备唯一标识',
  vendor       VARCHAR(64) NULL,
  model        VARCHAR(64) NULL,
  firmware     VARCHAR(32) NULL,
  range_min    DECIMAL(18,6) NULL,
  range_max    DECIMAL(18,6) NULL,
  net_status   VARCHAR(16) NOT NULL DEFAULT 'REGISTERED',
  status       VARCHAR(16) NOT NULL DEFAULT 'IN_USE',
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_device_code (tenant_id, device_code),
  UNIQUE KEY uk_device_sn (sn),
  KEY idx_device_station (station_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备台账';

CREATE TABLE IF NOT EXISTS md_station_factor (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT      NOT NULL,
  station_id  BIGINT      NOT NULL,
  factor_code VARCHAR(16) NOT NULL COMMENT 'HJ212 参数编码',
  device_id   BIGINT      NULL,
  unit        VARCHAR(16) NULL,
  range_min   DECIMAL(18,6) NULL,
  range_max   DECIMAL(18,6) NULL,
  std_limit   DECIMAL(18,6) NULL,
  is_main     TINYINT     NOT NULL DEFAULT 0,
  sort        INT         NOT NULL DEFAULT 0,
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_station_factor (station_id, factor_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站房因子配置';

CREATE TABLE IF NOT EXISTS alm_event (
  id              BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id       BIGINT      NOT NULL,
  event_no        VARCHAR(32) NOT NULL,
  rule_id         BIGINT      NOT NULL,
  station_id      BIGINT      NOT NULL,
  factor_code     VARCHAR(16) NULL,
  level           VARCHAR(4)  NOT NULL COMMENT 'P1/P2/P3',
  value           DECIMAL(18,6) NULL,
  threshold       DECIMAL(18,6) NULL,
  begin_at        DATETIME    NOT NULL,
  end_at          DATETIME    NULL,
  status          VARCHAR(16) NOT NULL DEFAULT 'OPEN',
  owner_id        BIGINT      NULL,
  related_task_id BIGINT      NULL,
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_event_no (event_no),
  KEY idx_event_station (station_id, level, status, begin_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警事件';

CREATE TABLE IF NOT EXISTS ops_task (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id    BIGINT      NOT NULL,
  task_no      VARCHAR(32) NOT NULL,
  station_id   BIGINT      NOT NULL,
  ops_type     VARCHAR(24) NOT NULL,
  source       VARCHAR(8)  NOT NULL DEFAULT 'PLAN',
  assignee_id  BIGINT      NOT NULL,
  dispatch_at  DATETIME    NULL,
  accept_at    DATETIME    NULL,
  arrive_at    DATETIME    NULL,
  finish_at    DATETIME    NULL,
  deadline     DATETIME    NULL,
  status       VARCHAR(16) NOT NULL DEFAULT 'DISPATCHED',
  is_overdue   TINYINT     NOT NULL DEFAULT 0,
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_no (task_no),
  KEY idx_task_assignee (assignee_id, status, deadline),
  KEY idx_task_station (station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维任务';

CREATE TABLE IF NOT EXISTS hj212_frame (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  mn          VARCHAR(64) NOT NULL,
  cn          VARCHAR(8)  NOT NULL,
  qn          VARCHAR(32) NOT NULL,
  crc_ok      TINYINT     NOT NULL DEFAULT 1,
  source      VARCHAR(8)  NOT NULL DEFAULT 'TCP',
  raw         TEXT        NULL,
  params      JSON        NULL,
  received_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_frame_mn (mn, received_at),
  KEY idx_frame_cn (cn, received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HJ212 原始报文';
