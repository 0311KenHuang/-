-- ============================================================
-- V5 停运区间
--
-- 附录 G：站房停运期间的数据不参与有效率与均值统计。
-- md_station 只存当前 biz_status 与原因，没有历史区间，无法回溯
-- "上个月停运那三天"到底从几点停到几点，所以单独建区间表。
--
-- 区间由站房状态切换自动维护：
--   切到 STOPPED 开区间（end_at 为空），切出 STOPPED 关闭区间。
-- end_at 为空表示"当前仍在停运"。
-- ============================================================

CREATE TABLE IF NOT EXISTS md_station_stop (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id  BIGINT       NOT NULL,
  station_id BIGINT       NOT NULL,
  begin_at   DATETIME     NOT NULL COMMENT '停运开始',
  end_at     DATETIME     NULL     COMMENT '停运结束，空表示仍在停运',
  reason     VARCHAR(255) NULL,
  source     VARCHAR(16)  NOT NULL DEFAULT 'STATUS' COMMENT 'STATUS 状态切换 / MANUAL 手工补录 / IMPORT',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_stop_station_begin (station_id, begin_at),
  KEY idx_stop_station_end (station_id, end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站房停运区间（附录 G 统计剔除依据）';

-- 日均有效率的分母要扣掉停运小时，把"应统计小时数"落下来便于报表解释
ALTER TABLE ts_data_day
  ADD COLUMN expected_hour_cnt INT NOT NULL DEFAULT 24
    COMMENT '应统计小时数 = 24 - 停运小时数' AFTER total_hour_cnt;
