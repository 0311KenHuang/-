-- =============================================================
-- 污染源自动监控设施第三方运维信息管理系统
-- 核心库表 DDL v1.0  (MySQL 8.0 / utf8mb4)
-- 说明：业务库不含分钟数据；分钟/小时/日数据见 TDengine 部分
-- =============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -------------------------------------------------------------
-- 0. 系统域
-- -------------------------------------------------------------
CREATE TABLE sys_tenant (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_code   VARCHAR(32)  NOT NULL COMMENT '租户编码',
  name          VARCHAR(100) NOT NULL COMMENT '租户名称',
  tenant_type   VARCHAR(16)  NOT NULL COMMENT 'REG 监管 / ORG 运维单位 / ENT 企业',
  status        TINYINT      NOT NULL DEFAULT 1,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_code (tenant_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户';

CREATE TABLE sys_region (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  parent_id  BIGINT      NOT NULL DEFAULT 0,
  code       VARCHAR(20) NOT NULL COMMENT '行政区划代码',
  name       VARCHAR(64) NOT NULL,
  level      TINYINT     NOT NULL COMMENT '1省 2市 3区县 4园区',
  sort       INT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_parent (parent_id),
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域';

CREATE TABLE sys_org (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id  BIGINT      NOT NULL,
  parent_id  BIGINT      NOT NULL DEFAULT 0,
  org_type   VARCHAR(16) NOT NULL COMMENT 'REG 监管 / OPS 运维单位 / ENT 企业',
  name       VARCHAR(120) NOT NULL,
  region_id  BIGINT      NULL,
  contact    VARCHAR(32) NULL,
  phone      VARCHAR(32) NULL,
  status     TINYINT     NOT NULL DEFAULT 1,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_tenant (tenant_id),
  KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织';

CREATE TABLE sys_user (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id      BIGINT      NOT NULL,
  org_id         BIGINT      NOT NULL,
  username       VARCHAR(64) NOT NULL,
  password_hash  VARCHAR(100) NOT NULL,
  real_name      VARCHAR(64) NOT NULL,
  phone          VARCHAR(32) NULL,
  email          VARCHAR(100) NULL,
  avatar         VARCHAR(255) NULL,
  cert_no        VARCHAR(64) NULL COMMENT '运维工资质证书号',
  cert_expire    DATE        NULL,
  status         TINYINT     NOT NULL DEFAULT 1 COMMENT '1正常 0停用 2锁定',
  last_login_at  DATETIME    NULL,
  created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (tenant_id, username),
  KEY idx_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

CREATE TABLE sys_role (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT      NOT NULL,
  role_code   VARCHAR(32) NOT NULL COMMENT 'SUP/REG/ORG/MGR/ENG/ENT',
  name        VARCHAR(64) NOT NULL,
  data_scope  VARCHAR(16) NOT NULL DEFAULT 'ORG' COMMENT 'ALL/REGION/ORG/SELF',
  remark      VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role (tenant_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色';

CREATE TABLE sys_menu (
  id        BIGINT      NOT NULL AUTO_INCREMENT,
  parent_id BIGINT      NOT NULL DEFAULT 0,
  name      VARCHAR(64) NOT NULL,
  path      VARCHAR(128) NULL,
  perms     VARCHAR(128) NULL COMMENT '权限标识 station:edit',
  menu_type CHAR(1)     NOT NULL COMMENT 'M目录 C菜单 B按钮',
  icon      VARCHAR(64) NULL,
  sort      INT         NOT NULL DEFAULT 0,
  visible   TINYINT     NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单/权限点';

CREATE TABLE sys_role_menu (
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单';

CREATE TABLE sys_dict (
  id        BIGINT      NOT NULL AUTO_INCREMENT,
  dict_code VARCHAR(64) NOT NULL,
  name      VARCHAR(64) NOT NULL,
  remark    VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dict (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典';

CREATE TABLE sys_dict_item (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  dict_code  VARCHAR(64) NOT NULL,
  item_value VARCHAR(64) NOT NULL,
  item_label VARCHAR(64) NOT NULL,
  sort       INT         NOT NULL DEFAULT 0,
  ext_json   JSON        NULL,
  PRIMARY KEY (id),
  KEY idx_dict (dict_code, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项';

-- 审计日志：只允许 INSERT
CREATE TABLE sys_audit_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT       NOT NULL,
  user_id     BIGINT       NOT NULL,
  user_name   VARCHAR(64)  NULL,
  module      VARCHAR(64)  NOT NULL,
  action      VARCHAR(32)  NOT NULL COMMENT 'CREATE/UPDATE/DELETE/EXPORT/LOGIN/CONTROL',
  biz_id      VARCHAR(64)  NULL,
  before_json JSON         NULL,
  after_json  JSON         NULL,
  ip          VARCHAR(64)  NULL,
  ua          VARCHAR(255) NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id, created_at),
  KEY idx_user_time (user_id, created_at),
  KEY idx_module (module, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志（只追加）'
  PARTITION BY RANGE (TO_DAYS(created_at)) (
    PARTITION p_init VALUES LESS THAN (TO_DAYS('2027-01-01')),
    PARTITION p_max  VALUES LESS THAN MAXVALUE
  );

CREATE TABLE sys_config (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id    BIGINT       NOT NULL DEFAULT 0,
  config_key   VARCHAR(64)  NOT NULL,
  config_value VARCHAR(1000) NULL,
  remark       VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cfg (tenant_id, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数';

-- -------------------------------------------------------------
-- 1. 主数据域
-- -------------------------------------------------------------
CREATE TABLE md_enterprise (
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
  contact       VARCHAR(32)  NULL,
  phone         VARCHAR(32)  NULL,
  is_key_unit   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否重点排污单位',
  status        TINYINT      NOT NULL DEFAULT 1,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by    BIGINT       NULL,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by    BIGINT       NULL,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  version       INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_credit (tenant_id, credit_code),
  KEY idx_region (region_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业档案';

CREATE TABLE md_station (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id        BIGINT       NOT NULL,
  station_code     VARCHAR(32)  NOT NULL COMMENT '业务编码 如 FHZY-S-01',
  name             VARCHAR(100) NOT NULL,
  station_type     VARCHAR(20)  NOT NULL COMMENT 'WASTE_WATER/WASTE_GAS/VOC/INCINERATION/AIR/GROUNDWATER',
  enterprise_id    BIGINT       NOT NULL,
  region_id        BIGINT       NOT NULL,
  lng              DECIMAL(10,7) NULL,
  lat              DECIMAL(10,7) NULL,
  address          VARCHAR(255) NULL,
  biz_status       VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/ABNORMAL/STOPPED/MAINTAINING',
  stop_reason      VARCHAR(255) NULL,
  ops_org_id       BIGINT       NULL COMMENT '责任运维单位',
  ops_engineer_id  BIGINT       NULL COMMENT '责任运维工程师',
  online_date      DATE         NULL,
  is_focus         TINYINT      NOT NULL DEFAULT 0,
  qr_token         VARCHAR(64)  NULL,
  remark           VARCHAR(500) NULL,
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by       BIGINT       NULL,
  updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by       BIGINT       NULL,
  deleted          TINYINT      NOT NULL DEFAULT 0,
  version          INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_station_code (tenant_id, station_code),
  KEY idx_ent (enterprise_id, station_type, biz_status),
  KEY idx_org (ops_org_id),
  KEY idx_region (region_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站房';

CREATE TABLE md_outlet (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id     BIGINT      NOT NULL,
  enterprise_id BIGINT      NOT NULL,
  outlet_code   VARCHAR(32) NOT NULL,
  name          VARCHAR(100) NOT NULL,
  outlet_type   VARCHAR(24) NULL COMMENT '废水总排口/废气排口/...',
  remark        VARCHAR(255) NULL,
  deleted       TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_outlet (tenant_id, enterprise_id, outlet_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排放口';

CREATE TABLE md_station_outlet (
  station_id BIGINT NOT NULL,
  outlet_id  BIGINT NOT NULL,
  PRIMARY KEY (station_id, outlet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站房-排口关联';

CREATE TABLE md_device (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id    BIGINT      NOT NULL,
  device_code  VARCHAR(32) NOT NULL,
  station_id   BIGINT      NOT NULL,
  category     VARCHAR(16) NOT NULL COMMENT 'ANALYZER/DTU/FLOW/OTHER',
  sn           VARCHAR(64) NULL COMMENT '设备唯一标识',
  vendor_code  VARCHAR(32) NULL,
  cpu_id       VARCHAR(64) NULL,
  mac_addr     VARCHAR(64) NULL,
  vendor       VARCHAR(64) NULL,
  model        VARCHAR(64) NULL,
  ccep_no      VARCHAR(64) NULL,
  firmware     VARCHAR(32) NULL,
  range_min    DECIMAL(18,6) NULL,
  range_max    DECIMAL(18,6) NULL,
  install_date DATE        NULL,
  net_status   VARCHAR(16) NOT NULL DEFAULT 'REGISTERED' COMMENT 'REGISTERED/ACTIVATED/CONFIRMED',
  status       VARCHAR(16) NOT NULL DEFAULT 'IN_USE' COMMENT 'IN_USE/STOPPED/REPLACED/SCRAPPED',
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sn (sn),
  KEY idx_station (station_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备台账';

CREATE TABLE md_factor (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  factor_code  VARCHAR(16) NOT NULL COMMENT 'HJ212 参数编码 如 a21026',
  name         VARCHAR(64) NOT NULL COMMENT '化学需氧量',
  alias        VARCHAR(32) NULL,
  unit         VARCHAR(16) NULL COMMENT 'mg/L',
  category     VARCHAR(16) NULL COMMENT 'WATER/GAS/OTHER',
  default_min  DECIMAL(18,6) NULL,
  default_max  DECIMAL(18,6) NULL,
  sort         INT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_factor (factor_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='因子字典';

CREATE TABLE md_station_factor (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT      NOT NULL,
  station_id  BIGINT      NOT NULL,
  factor_code VARCHAR(16) NOT NULL,
  device_id   BIGINT      NULL,
  unit        VARCHAR(16) NULL,
  range_min   DECIMAL(18,6) NULL,
  range_max   DECIMAL(18,6) NULL,
  std_limit   DECIMAL(18,6) NULL COMMENT '执行限值',
  is_main     TINYINT     NOT NULL DEFAULT 0 COMMENT '是否主因子（卡片展示）',
  sort        INT         NOT NULL DEFAULT 0,
  deleted     TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sf (station_id, factor_code),
  KEY idx_factor (factor_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站房因子配置';

CREATE TABLE md_standard (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  std_code      VARCHAR(32) NOT NULL,
  name          VARCHAR(160) NOT NULL,
  industry_code VARCHAR(16) NULL,
  medium        VARCHAR(8)  NULL COMMENT 'WATER/GAS',
  PRIMARY KEY (id),
  UNIQUE KEY uk_std (std_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行标准';

CREATE TABLE md_standard_limit (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  std_code       VARCHAR(32) NOT NULL,
  factor_code    VARCHAR(16) NOT NULL,
  limit_type     VARCHAR(16) NOT NULL COMMENT 'DAY/HOUR/INSTANT',
  limit_value    DECIMAL(18,6) NOT NULL,
  effective_from DATE        NOT NULL,
  effective_to   DATE        NULL,
  PRIMARY KEY (id),
  KEY idx_std_factor (std_code, factor_code, effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准限值（分时段）';

CREATE TABLE md_contract (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id     BIGINT      NOT NULL,
  contract_no   VARCHAR(64) NOT NULL,
  org_id        BIGINT      NOT NULL COMMENT '运维单位',
  enterprise_id BIGINT      NOT NULL,
  start_date    DATE        NOT NULL,
  end_date      DATE        NOT NULL,
  sla_json      JSON        NULL COMMENT '{"response_min":30,"arrive_min":120,"fix_min":1440}',
  amount        DECIMAL(14,2) NULL,
  status        VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted       TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_contract (tenant_id, contract_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维合同';

CREATE TABLE md_contract_station (
  contract_id BIGINT NOT NULL,
  station_id  BIGINT NOT NULL,
  PRIMARY KEY (contract_id, station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同站房';

-- -------------------------------------------------------------
-- 2. 运维域
-- -------------------------------------------------------------
CREATE TABLE ops_plan (
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
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by   BIGINT      NULL,
  deleted      TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_plan_no (plan_no),
  KEY idx_station_period (station_id, period),
  KEY idx_period_status (period, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维计划';

CREATE TABLE ops_plan_item (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  plan_id     BIGINT      NOT NULL,
  ops_type    VARCHAR(24) NOT NULL COMMENT '巡检/校准/校验/标液核查/试剂更换/物件更换/故障报告/故障维修',
  plan_date   DATE        NOT NULL,
  assignee_id BIGINT      NULL,
  done        TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_plan (plan_id),
  KEY idx_assignee (assignee_id, plan_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划明细';

CREATE TABLE ops_task (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id    BIGINT      NOT NULL,
  task_no      VARCHAR(32) NOT NULL,
  plan_item_id BIGINT      NULL COMMENT '故障类任务可为空',
  station_id   BIGINT      NOT NULL,
  ops_type     VARCHAR(24) NOT NULL,
  source       VARCHAR(8)  NOT NULL DEFAULT 'PLAN' COMMENT 'PLAN/ALARM/MANUAL',
  assignee_id  BIGINT      NOT NULL,
  dispatch_at  DATETIME    NULL,
  accept_at    DATETIME    NULL,
  arrive_at    DATETIME    NULL,
  finish_at    DATETIME    NULL,
  deadline     DATETIME    NULL,
  sla_json     JSON        NULL COMMENT 'SLA 快照，避免合同变更影响历史',
  status       VARCHAR(16) NOT NULL DEFAULT 'DISPATCHED',
  is_overdue   TINYINT     NOT NULL DEFAULT 0,
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_no (task_no),
  KEY idx_assignee (assignee_id, status, deadline),
  KEY idx_station (station_id, plan_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维任务';

CREATE TABLE ops_task_log (
  id        BIGINT      NOT NULL AUTO_INCREMENT,
  task_id   BIGINT      NOT NULL,
  action    VARCHAR(16) NOT NULL COMMENT 'DISPATCH/ACCEPT/ARRIVE/SUBMIT/TRANSFER/RETURN/CLOSE',
  operator  BIGINT      NOT NULL,
  remark    VARCHAR(255) NULL,
  created_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_task (task_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务流转日志';

CREATE TABLE ops_record (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id     BIGINT      NOT NULL,
  record_no     VARCHAR(32) NOT NULL,
  task_id       BIGINT      NOT NULL,
  station_id    BIGINT      NOT NULL,
  ops_type      VARCHAR(24) NOT NULL,
  engineer_id   BIGINT      NOT NULL,
  begin_time    DATETIME    NOT NULL,
  end_time      DATETIME    NULL,
  mark_begin    DATETIME    NULL COMMENT '应产生的数据标记起点',
  mark_end      DATETIME    NULL COMMENT '应产生的数据标记终点',
  conclusion    VARCHAR(500) NULL,
  next_plan_hint DATE       NULL,
  status        VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/APPROVED/REJECTED',
  sign_engineer VARCHAR(255) NULL,
  sign_auditor  VARCHAR(255) NULL,
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_record_no (record_no),
  UNIQUE KEY uk_task (task_id),
  KEY idx_station_type_time (station_id, ops_type, begin_time),
  KEY idx_status (status, begin_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维记录';

CREATE TABLE ops_record_item (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  record_id   BIGINT       NOT NULL,
  item_code   VARCHAR(32)  NOT NULL,
  item_name   VARCHAR(128) NOT NULL,
  value_type  VARCHAR(16)  NOT NULL COMMENT 'NUMBER/TEXT/SELECT/PHOTO/SIGN',
  value       VARCHAR(1000) NULL,
  unit        VARCHAR(16)  NULL,
  normal_flag TINYINT      NULL COMMENT '1正常 0异常',
  PRIMARY KEY (id),
  KEY idx_record (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维记录明细';

CREATE TABLE ops_record_media (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  record_id      BIGINT      NOT NULL,
  media_type     VARCHAR(8)  NOT NULL COMMENT 'PHOTO/VIDEO/FILE',
  object_key     VARCHAR(255) NOT NULL,
  watermark_json JSON        NULL COMMENT '{time,lng,lat,station,user}',
  sha256         CHAR(64)    NULL,
  created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_record (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维多媒体';

CREATE TABLE ops_checkin (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  task_id     BIGINT      NOT NULL,
  user_id     BIGINT      NOT NULL,
  station_id  BIGINT      NOT NULL,
  lng         DECIMAL(10,7) NULL,
  lat         DECIMAL(10,7) NULL,
  distance_m  INT         NULL COMMENT '与站房距离(米)',
  photo_key   VARCHAR(255) NULL,
  in_at       DATETIME    NULL,
  out_at      DATETIME    NULL,
  is_valid    TINYINT     NOT NULL DEFAULT 1 COMMENT '距离<=300m 且人脸通过',
  PRIMARY KEY (id),
  KEY idx_task (task_id),
  KEY idx_user_time (user_id, in_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='现场打卡';

CREATE TABLE ops_audit (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  record_id  BIGINT      NOT NULL,
  auditor_id BIGINT      NOT NULL,
  result     VARCHAR(8)  NOT NULL COMMENT 'PASS/REJECT',
  reason     VARCHAR(500) NULL,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_record (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维审核';

CREATE TABLE ops_template (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id    BIGINT      NOT NULL,
  station_type VARCHAR(20) NOT NULL,
  ops_type     VARCHAR(24) NOT NULL,
  name         VARCHAR(100) NOT NULL,
  version      INT         NOT NULL DEFAULT 1,
  enabled      TINYINT     NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_type (station_type, ops_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业模板';

CREATE TABLE ops_template_item (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  template_id BIGINT      NOT NULL,
  item_code  VARCHAR(32)  NOT NULL,
  label      VARCHAR(128) NOT NULL,
  value_type VARCHAR(16)  NOT NULL,
  required   TINYINT      NOT NULL DEFAULT 1,
  std_value  VARCHAR(64)  NULL,
  unit       VARCHAR(16)  NULL,
  sort       INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_template (template_id, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业模板项';

CREATE TABLE ops_attendance (
  id           BIGINT   NOT NULL AUTO_INCREMENT,
  user_id      BIGINT   NOT NULL,
  work_date    DATE     NOT NULL,
  check_in     DATETIME NULL,
  check_out    DATETIME NULL,
  work_hours   DECIMAL(5,2) NULL,
  on_site_hours DECIMAL(5,2) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_date (user_id, work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤';

CREATE TABLE ops_vehicle (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT      NOT NULL,
  org_id      BIGINT      NOT NULL,
  plate_no    VARCHAR(16) NOT NULL,
  model       VARCHAR(64) NULL,
  owner_id    BIGINT      NULL,
  terminal_sn VARCHAR(64) NULL COMMENT 'GPS/北斗终端号',
  deleted     TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_plate (tenant_id, plate_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维车辆';

-- -------------------------------------------------------------
-- 3. 物资域
-- -------------------------------------------------------------
CREATE TABLE inv_item (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id      BIGINT      NOT NULL,
  item_code      VARCHAR(32) NOT NULL,
  name           VARCHAR(128) NOT NULL,
  category       VARCHAR(16) NOT NULL COMMENT 'REAGENT 试剂 / STANDARD 标液 / SPARE 备件 / CONSUMABLE 耗材',
  spec           VARCHAR(64) NULL,
  unit           VARCHAR(16) NULL,
  shelf_life_days INT        NULL,
  safety_stock   DECIMAL(12,2) NULL,
  deleted        TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_item (tenant_id, item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物资档案';

CREATE TABLE inv_warehouse (
  id        BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT      NOT NULL,
  org_id    BIGINT      NOT NULL,
  name      VARCHAR(100) NOT NULL,
  address   VARCHAR(255) NULL,
  keeper_id BIGINT      NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库';

CREATE TABLE inv_stock (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  warehouse_id BIGINT      NOT NULL,
  item_id      BIGINT      NOT NULL,
  batch_no     VARCHAR(64) NULL,
  qty          DECIMAL(12,2) NOT NULL DEFAULT 0,
  expire_date  DATE        NULL,
  updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_stock (warehouse_id, item_id, batch_no),
  KEY idx_expire (expire_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存';

CREATE TABLE inv_flow (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT      NOT NULL,
  item_id     BIGINT      NOT NULL,
  warehouse_id BIGINT     NOT NULL,
  batch_no    VARCHAR(64) NULL,
  flow_type   VARCHAR(8)  NOT NULL COMMENT 'IN/OUT/RETURN/SCRAP',
  qty         DECIMAL(12,2) NOT NULL,
  station_id  BIGINT      NULL COMMENT '去向站房',
  device_id   BIGINT      NULL COMMENT '去向设备',
  task_id     BIGINT      NULL COMMENT '关联任务，实现流向追溯',
  operator    BIGINT      NOT NULL,
  remark      VARCHAR(255) NULL,
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_item_time (item_id, created_at),
  KEY idx_task (task_id),
  KEY idx_station (station_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出入库流水';

CREATE TABLE inv_apply (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT      NOT NULL,
  apply_no    VARCHAR(32) NOT NULL,
  applicant_id BIGINT     NOT NULL,
  station_id  BIGINT      NULL,
  task_id     BIGINT      NULL,
  status      VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/APPROVED/REJECTED/ISSUED',
  approver_id BIGINT      NULL,
  approved_at DATETIME    NULL,
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_apply_no (apply_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物资领用申请';

CREATE TABLE inv_apply_item (
  id       BIGINT NOT NULL AUTO_INCREMENT,
  apply_id BIGINT NOT NULL,
  item_id  BIGINT NOT NULL,
  qty      DECIMAL(12,2) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_apply (apply_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领用明细';

-- -------------------------------------------------------------
-- 4. 报警与消息域
-- -------------------------------------------------------------
CREATE TABLE alm_rule (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id     BIGINT      NOT NULL,
  rule_code     VARCHAR(32) NOT NULL,
  name          VARCHAR(100) NOT NULL,
  category      VARCHAR(16) NOT NULL COMMENT 'DATA/VALIDITY/DEVICE/OPS/SECURITY',
  level         VARCHAR(4)  NOT NULL COMMENT 'P1/P2/P3',
  expression    VARCHAR(1000) NOT NULL,
  duration_sec  INT         NOT NULL DEFAULT 0 COMMENT '持续满足时长，防抖',
  suppress_sec  INT         NOT NULL DEFAULT 0 COMMENT '抑制窗口',
  window_json   JSON        NULL COMMENT '生效时段/日期/站房类型',
  channels      VARCHAR(64) NULL COMMENT 'FEISHU,WXA,SMS',
  escalate_json JSON        NULL,
  enabled       TINYINT     NOT NULL DEFAULT 1,
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rule (tenant_id, rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警规则';

CREATE TABLE alm_rule_target (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  rule_id     BIGINT      NOT NULL,
  station_id  BIGINT      NULL,
  factor_code VARCHAR(16) NULL,
  ops_type    VARCHAR(24) NULL,
  PRIMARY KEY (id),
  KEY idx_rule (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则适用对象';

CREATE TABLE alm_event (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id      BIGINT      NOT NULL,
  event_no       VARCHAR(32) NOT NULL,
  rule_id        BIGINT      NOT NULL,
  station_id     BIGINT      NOT NULL,
  factor_code    VARCHAR(16) NULL,
  level          VARCHAR(4)  NOT NULL,
  value          DECIMAL(18,6) NULL,
  threshold      DECIMAL(18,6) NULL,
  begin_at       DATETIME    NOT NULL,
  end_at         DATETIME    NULL,
  status         VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/ACK/PROCESSING/CLOSED/FALSE_ALARM',
  owner_id       BIGINT      NULL,
  related_task_id BIGINT     NULL,
  evidence_json  JSON        NULL COMMENT '曲线区间/报文/照片引用',
  created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id, begin_at),
  UNIQUE KEY uk_event_no (event_no),
  KEY idx_station (station_id, level, status, begin_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警事件'
  PARTITION BY RANGE (TO_DAYS(begin_at)) (
    PARTITION p_init VALUES LESS THAN (TO_DAYS('2027-01-01')),
    PARTITION p_max  VALUES LESS THAN MAXVALUE
  );

CREATE TABLE alm_action (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  event_id   BIGINT      NOT NULL,
  action     VARCHAR(16) NOT NULL COMMENT 'ACK/TRANSFER/PROCESS/CLOSE/FALSE_ALARM',
  operator   BIGINT      NOT NULL,
  remark     VARCHAR(500) NULL,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_event (event_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警处置';

CREATE TABLE msg_template (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT      NOT NULL,
  event_type  VARCHAR(32) NOT NULL COMMENT 'ALARM_P1/OPS_OVERDUE/RECTIFY/...',
  channel     VARCHAR(16) NOT NULL COMMENT 'FEISHU/MP/WXA/SMS/INNER',
  title_tpl   VARCHAR(200) NULL,
  body_tpl    TEXT        NULL,
  card_json   JSON        NULL COMMENT '飞书交互卡片',
  enabled     TINYINT     NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tpl (tenant_id, event_type, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板';

CREATE TABLE msg_channel (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT      NOT NULL,
  channel     VARCHAR(16) NOT NULL,
  app_id      VARCHAR(64) NULL,
  secret_ref  VARCHAR(128) NULL COMMENT '密钥引用，不存明文',
  config_json JSON        NULL,
  enabled     TINYINT     NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_channel (tenant_id, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道配置';

CREATE TABLE msg_send_log (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id     BIGINT      NOT NULL,
  event_id      BIGINT      NULL,
  channel       VARCHAR(16) NOT NULL,
  receiver      VARCHAR(128) NULL,
  template_code VARCHAR(32) NULL,
  status        VARCHAR(12) NOT NULL COMMENT 'SENT/FAILED/RETRY',
  retry_cnt     INT         NOT NULL DEFAULT 0,
  error_msg     VARCHAR(500) NULL,
  sent_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id, sent_at),
  KEY idx_event (event_id, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息发送日志'
  PARTITION BY RANGE (TO_DAYS(sent_at)) (
    PARTITION p_init VALUES LESS THAN (TO_DAYS('2027-01-01')),
    PARTITION p_max  VALUES LESS THAN MAXVALUE
  );

CREATE TABLE msg_subscribe (
  id               BIGINT      NOT NULL AUTO_INCREMENT,
  user_id          BIGINT      NOT NULL,
  channel          VARCHAR(16) NOT NULL,
  openid           VARCHAR(64) NULL,
  unionid          VARCHAR(64) NULL,
  subscribed_types VARCHAR(255) NULL,
  bind_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_channel (user_id, channel),
  KEY idx_openid (openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信/飞书订阅绑定';

CREATE TABLE msg_duty (
  id      BIGINT   NOT NULL AUTO_INCREMENT,
  org_id  BIGINT   NOT NULL,
  duty_date DATE   NOT NULL,
  shift   VARCHAR(16) NOT NULL COMMENT 'DAY/NIGHT',
  user_id BIGINT   NOT NULL,
  phone   VARCHAR(32) NULL,
  PRIMARY KEY (id),
  KEY idx_org_date (org_id, duty_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='值班表';

-- -------------------------------------------------------------
-- 5. 设备运行数据（关系库存元数据，明细在时序库）
-- -------------------------------------------------------------
CREATE TABLE dev_param_snapshot (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  device_id   BIGINT      NOT NULL,
  param_code  VARCHAR(32) NOT NULL COMMENT 'RANGE_MIN/RANGE_MAX/SLOPE/INTERCEPT/DIGEST_TEMP/...',
  param_value VARCHAR(128) NULL,
  collected_at DATETIME   NOT NULL,
  source      VARCHAR(16) NOT NULL DEFAULT 'DTU',
  PRIMARY KEY (id),
  KEY idx_device (device_id, param_code, collected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作参数快照';

CREATE TABLE dev_param_change (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id    BIGINT      NOT NULL,
  device_id    BIGINT      NOT NULL,
  param_code   VARCHAR(32) NOT NULL,
  old_value    VARCHAR(128) NULL,
  new_value    VARCHAR(128) NULL,
  change_time  DATETIME    NOT NULL,
  operator     BIGINT      NULL,
  evidence_key VARCHAR(255) NULL COMMENT '截图/日志文件',
  PRIMARY KEY (id),
  KEY idx_device (device_id, change_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数变更留痕';

CREATE TABLE dev_online_log (
  id              BIGINT      NOT NULL AUTO_INCREMENT,
  station_id      BIGINT      NOT NULL,
  dtu_sn          VARCHAR(64) NULL,
  last_heartbeat  DATETIME    NULL,
  online          TINYINT     NOT NULL DEFAULT 0,
  offline_minutes INT         NOT NULL DEFAULT 0,
  updated_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_station (station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联网状态';

CREATE TABLE dev_key_state (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  device_sn   VARCHAR(64) NOT NULL,
  key_version VARCHAR(32) NULL,
  updated_at  DATETIME    NULL,
  expire_at   DATETIME    NULL,
  fail_count  INT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sn (device_sn),
  KEY idx_expire (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密钥状态';

CREATE TABLE ts_qc_check (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id      BIGINT      NOT NULL,
  station_id     BIGINT      NOT NULL,
  factor_code    VARCHAR(16) NOT NULL,
  check_type     VARCHAR(24) NOT NULL COMMENT '标液核查/校准/比对监测',
  std_value      DECIMAL(18,6) NULL,
  measured_value DECIMAL(18,6) NULL,
  deviation      DECIMAL(8,4) NULL COMMENT '相对误差 %',
  passed         TINYINT     NULL,
  check_time     DATETIME    NOT NULL,
  operator       BIGINT      NULL,
  record_id      BIGINT      NULL COMMENT '关联运维记录',
  PRIMARY KEY (id),
  KEY idx_station (station_id, factor_code, check_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质控记录';

CREATE TABLE ts_missing (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id   BIGINT      NOT NULL,
  station_id  BIGINT      NOT NULL,
  factor_code VARCHAR(16) NOT NULL,
  begin_time  DATETIME    NOT NULL,
  end_time    DATETIME    NOT NULL,
  minutes     INT         NOT NULL,
  reason      VARCHAR(16) NULL COMMENT 'POWER/NET/STOP/FAULT/UNKNOWN',
  recalled    TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_station (station_id, begin_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据缺失记录';

-- -------------------------------------------------------------
-- 6. 监管域
-- -------------------------------------------------------------
CREATE TABLE law_inspection (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id     BIGINT      NOT NULL,
  inspect_no    VARCHAR(32) NOT NULL,
  enterprise_id BIGINT      NOT NULL,
  station_id    BIGINT      NULL,
  inspector_id  BIGINT      NOT NULL,
  inspect_at    DATETIME    NOT NULL,
  result        VARCHAR(16) NULL COMMENT 'PASS/ISSUE',
  items_json    JSON        NULL,
  remark        VARCHAR(500) NULL,
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_inspect_no (inspect_no),
  KEY idx_ent (enterprise_id, inspect_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执法检查';

CREATE TABLE law_rectify (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id     BIGINT      NOT NULL,
  notice_no     VARCHAR(32) NOT NULL,
  inspection_id BIGINT      NULL,
  enterprise_id BIGINT      NOT NULL,
  station_id    BIGINT      NULL,
  issue         VARCHAR(1000) NOT NULL,
  basis         VARCHAR(500) NULL COMMENT '法律/标准依据',
  deadline      DATE        NOT NULL,
  status        VARCHAR(16) NOT NULL DEFAULT 'ISSUED' COMMENT 'ISSUED/REPLIED/REVIEWING/CLOSED/OVERDUE',
  created_by    BIGINT      NOT NULL,
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notice_no (notice_no),
  KEY idx_ent (enterprise_id, status, deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='整改通知单';

CREATE TABLE law_rectify_reply (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  notice_id     BIGINT      NOT NULL,
  content       TEXT        NULL,
  files_json    JSON        NULL,
  reply_at      DATETIME    NOT NULL,
  reviewer_id   BIGINT      NULL,
  review_result VARCHAR(16) NULL COMMENT 'PASS/REJECT',
  review_remark VARCHAR(500) NULL,
  PRIMARY KEY (id),
  KEY idx_notice (notice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='整改回复';

-- -------------------------------------------------------------
-- 7. 档案域
-- -------------------------------------------------------------
CREATE TABLE doc_file (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id     BIGINT      NOT NULL,
  category      VARCHAR(24) NOT NULL COMMENT 'CONTRACT/ACCEPTANCE/COMPARE/PERMIT/RECTIFY/LEDGER',
  enterprise_id BIGINT      NULL,
  station_id    BIGINT      NULL,
  name          VARCHAR(200) NOT NULL,
  object_key    VARCHAR(255) NOT NULL,
  size_bytes    BIGINT      NULL,
  sha256        CHAR(64)    NULL,
  expire_date   DATE        NULL,
  version       INT         NOT NULL DEFAULT 1,
  uploader      BIGINT      NOT NULL,
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted       TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_station (station_id, category),
  KEY idx_ent (enterprise_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='档案文件';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================
-- TDengine 部分（时序数据）
-- =============================================================
-- CREATE DATABASE env_mon KEEP 365 DURATION 10 PRECISION 'ms';
--
-- CREATE STABLE ts_data_minute (
--   ts        TIMESTAMP,
--   value     DOUBLE,
--   dev_flag  NCHAR(4),
--   prod_flag NCHAR(4),
--   raw_value DOUBLE,
--   quality   TINYINT
-- ) TAGS (station_id BIGINT, station_code NCHAR(32), factor_code NCHAR(16), unit NCHAR(16), tenant_id BIGINT);
--
-- CREATE STABLE ts_data_hour (
--   ts        TIMESTAMP,
--   avg_value DOUBLE,
--   max_value DOUBLE,
--   min_value DOUBLE,
--   valid_cnt INT,
--   total_cnt INT,
--   flag      NCHAR(4)
-- ) TAGS (station_id BIGINT, factor_code NCHAR(16), tenant_id BIGINT);
--
-- CREATE STABLE ts_data_day (
--   ts         TIMESTAMP,
--   avg_value  DOUBLE,
--   max_value  DOUBLE,
--   min_value  DOUBLE,
--   emission   DOUBLE,
--   valid_rate DOUBLE
-- ) TAGS (station_id BIGINT, factor_code NCHAR(16), tenant_id BIGINT);
--
-- CREATE STABLE ts_raw_signal (
--   ts           TIMESTAMP,
--   signal_value DOUBLE,
--   zero_signal  DOUBLE,
--   span_signal  DOUBLE,
--   peak_area    DOUBLE,
--   sample_no    NCHAR(32)
-- ) TAGS (station_id BIGINT, device_sn NCHAR(64), factor_code NCHAR(16), tenant_id BIGINT);
