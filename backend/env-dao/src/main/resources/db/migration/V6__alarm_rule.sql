-- ============================================================
-- V6 报警规则与处置记录
--
-- 规则模型沿用 docs/04：规则码 + 级别 + 持续时长 + 抑制窗口 + 参数。
-- 本期内置三条数据类规则（超标 / 恒值 / 缺数），求值逻辑按 rule_code 走代码，
-- params_json 只放阈值参数，后续加规则继续沿用这张表。
-- ============================================================

CREATE TABLE IF NOT EXISTS alm_rule (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id     BIGINT       NOT NULL,
  rule_code     VARCHAR(32)  NOT NULL COMMENT 'DATA_OVER_LIMIT / DATA_DEAD_VALUE / DATA_MISSING',
  name          VARCHAR(100) NOT NULL,
  category      VARCHAR(16)  NOT NULL COMMENT 'DATA/VALIDITY/DEVICE/OPS/SECURITY',
  level         VARCHAR(4)   NOT NULL COMMENT 'P1/P2/P3',
  expression    VARCHAR(1000) NULL COMMENT '通用表达式，内置规则暂时按 rule_code 求值',
  duration_sec  INT          NOT NULL DEFAULT 0 COMMENT '条件需持续秒数',
  suppress_sec  INT          NOT NULL DEFAULT 0 COMMENT '同对象同规则抑制窗口',
  params_json   JSON         NULL COMMENT '内置规则参数：consecutive / gap_min',
  window_json   JSON         NULL COMMENT '生效时段/站房类型',
  channels      VARCHAR(64)  NULL COMMENT 'FEISHU,WXA,SMS',
  escalate_json JSON         NULL,
  enabled       TINYINT      NOT NULL DEFAULT 1,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rule (tenant_id, rule_code),
  KEY idx_rule_enabled (tenant_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警规则';

CREATE TABLE IF NOT EXISTS alm_action (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id  BIGINT       NOT NULL,
  event_id   BIGINT       NOT NULL,
  action     VARCHAR(16)  NOT NULL COMMENT 'ACK/CLOSE/FALSE_ALARM/AUTO_CLOSE',
  operator   BIGINT       NOT NULL COMMENT '操作人，自动关闭为 0',
  remark     VARCHAR(500) NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_action_event (event_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警处置记录';

-- 内置规则：按现有租户初始化，后续新建租户由引擎启动时补齐
INSERT INTO alm_rule (tenant_id, rule_code, name, category, level,
                      duration_sec, suppress_sec, params_json, channels, enabled, deleted)
SELECT t.id, 'DATA_OVER_LIMIT', '污染物超标', 'DATA', 'P1',
       180, 3600, '{"consecutive":3}', 'FEISHU', 1, 0
FROM sys_tenant t WHERE t.deleted = 0;

INSERT INTO alm_rule (tenant_id, rule_code, name, category, level,
                      duration_sec, suppress_sec, params_json, channels, enabled, deleted)
SELECT t.id, 'DATA_DEAD_VALUE', '数据恒值/保持', 'DATA', 'P2',
       0, 14400, '{"consecutive":10}', 'FEISHU', 1, 0
FROM sys_tenant t WHERE t.deleted = 0;

INSERT INTO alm_rule (tenant_id, rule_code, name, category, level,
                      duration_sec, suppress_sec, params_json, channels, enabled, deleted)
SELECT t.id, 'DATA_MISSING', '数据缺失', 'DATA', 'P1',
       0, 3600, '{"gap_min":30}', 'FEISHU', 1, 0
FROM sys_tenant t WHERE t.deleted = 0;
