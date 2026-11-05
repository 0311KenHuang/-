-- ============================================================
-- V7 消息中心：模板 / 发送日志 / 值班表
--
-- 对应 docs/04 §6：报警事件 → 选模板 → 解析接收人 → 渠道决策 → 发送 → 留痕。
-- 每次发送（含跳过与降级）都写 msg_send_log，触达率可查、可追责。
-- 微信生态的硬限制（订阅授权、模板报备）见文档，本期适配器按配置开关接入。
-- ============================================================

CREATE TABLE IF NOT EXISTS msg_template (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id  BIGINT       NOT NULL,
  event_type VARCHAR(32)  NOT NULL COMMENT 'ALARM_P1/ALARM_P2/ALARM_RECOVER/ALARM_ESCALATE/...',
  channel    VARCHAR(16)  NOT NULL COMMENT 'FEISHU/WXA/SMS/INNER',
  title_tpl  VARCHAR(200) NULL,
  body_tpl   TEXT         NULL,
  card_json  JSON         NULL COMMENT '飞书交互卡片模板（预留）',
  enabled    TINYINT      NOT NULL DEFAULT 1,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tpl (tenant_id, event_type, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板';

CREATE TABLE IF NOT EXISTS msg_send_log (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id     BIGINT       NOT NULL,
  event_id      BIGINT       NULL,
  channel       VARCHAR(16)  NOT NULL,
  receiver      VARCHAR(128) NULL,
  template_code VARCHAR(32)  NULL,
  status        VARCHAR(12)  NOT NULL COMMENT 'SENT/FAILED/SKIPPED/RETRY',
  stage         INT          NOT NULL DEFAULT 0 COMMENT '0 首报 / 1 一次升级 / 2 二次升级',
  retry_cnt     INT          NOT NULL DEFAULT 0,
  error_msg     VARCHAR(500) NULL,
  cost_ms       INT          NOT NULL DEFAULT 0,
  content       VARCHAR(1000) NULL,
  sent_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_log_event (event_id, channel),
  KEY idx_log_sent (sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息发送日志（只追加）';

CREATE TABLE IF NOT EXISTS msg_duty (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  tenant_id  BIGINT      NOT NULL,
  org_id     BIGINT      NOT NULL,
  duty_date  DATE        NOT NULL,
  shift      VARCHAR(16) NOT NULL COMMENT 'DAY/NIGHT',
  user_id    BIGINT      NOT NULL,
  phone      VARCHAR(32) NULL,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_duty (tenant_id, org_id, duty_date, shift)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='值班表（接收人解析第二优先级）';

-- 升级阶段跟着事件走，避免定时任务重复推同一级
ALTER TABLE alm_event
  ADD COLUMN escalate_stage INT NOT NULL DEFAULT 0 COMMENT '0 首报 / 1 T+15min / 2 T+30min' AFTER status;

-- 内置模板：按现有租户初始化，新建租户由模板服务用代码默认值兜底
INSERT INTO msg_template (tenant_id, event_type, channel, title_tpl, body_tpl, enabled, deleted)
SELECT t.id, x.event_type, x.channel, x.title_tpl, x.body_tpl, 1, 0
FROM sys_tenant t
CROSS JOIN (
  SELECT 'ALARM_P1' AS event_type, 'FEISHU' AS channel,
         '【{{level}}】{{ruleName}} · {{stationName}}' AS title_tpl,
         '因子 {{factorCode}} 当前值 {{value}}，限值 {{threshold}}，发生时间 {{beginAt}}，事件号 {{eventNo}}' AS body_tpl
  UNION ALL SELECT 'ALARM_P1', 'SMS',
         '【{{level}}】{{ruleName}}',
         '{{stationName}} 因子 {{factorCode}} 值 {{value}}（限值 {{threshold}}），时间 {{beginAt}}，事件 {{eventNo}}'
  UNION ALL SELECT 'ALARM_P1', 'WXA',
         '【{{level}}】{{ruleName}}',
         '{{stationName}} · {{factorCode}} · {{value}}，时间 {{beginAt}}'
  UNION ALL SELECT 'ALARM_P2', 'FEISHU',
         '【{{level}}】{{ruleName}} · {{stationName}}',
         '因子 {{factorCode}} 当前值 {{value}}，限值 {{threshold}}，发生时间 {{beginAt}}，事件号 {{eventNo}}'
  UNION ALL SELECT 'ALARM_P2', 'WXA',
         '【{{level}}】{{ruleName}}',
         '{{stationName}} · {{factorCode}} · {{value}}，时间 {{beginAt}}'
  UNION ALL SELECT 'ALARM_P3', 'INNER',
         '【{{level}}】{{ruleName}}',
         '{{stationName}} 因子 {{factorCode}} 值 {{value}}，时间 {{beginAt}}'
  UNION ALL SELECT 'ALARM_RECOVER', 'INNER',
         '【恢复】{{ruleName}} · {{stationName}}',
         '因子 {{factorCode}} 已恢复，事件 {{eventNo}} 已自动关闭'
  UNION ALL SELECT 'ALARM_ESCALATE', 'FEISHU',
         '【升级 T+{{stage}}】{{ruleName}} · {{stationName}}',
         '事件 {{eventNo}} 仍未认领，因子 {{factorCode}} 值 {{value}}，请尽快处置'
) x
WHERE t.deleted = 0;
