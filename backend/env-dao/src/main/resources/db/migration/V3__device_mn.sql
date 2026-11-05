-- ============================================================
-- V3 直连数采仪：建立 MN（站点编号）→ 站房 的归属关系
--
-- HJ212 报文头里的 MN 是现场机唯一编号，直连场景下必须能反查到站房，
-- 否则数据落库了也不知道属于谁。
-- ============================================================

ALTER TABLE md_device
  ADD COLUMN mn VARCHAR(64) NULL COMMENT '数采仪站点编号（HJ212 报文 MN）' AFTER sn;

ALTER TABLE md_device
  ADD UNIQUE KEY uk_device_mn (mn);

ALTER TABLE hj212_frame
  ADD COLUMN station_id BIGINT NULL COMMENT '归属站房（按 MN 解析，未注册为空）' AFTER qn;

ALTER TABLE hj212_frame
  ADD KEY idx_frame_station (station_id, received_at);

-- 演示用数采仪：MN 与站房一一对应
INSERT INTO md_device (tenant_id, device_code, station_id, category, sn, mn,
                       vendor, model, firmware, net_status, status, deleted) VALUES
  (1, 'DTU-FHZY-S-01', 1, 'DTU', 'SN-FHZY-S-01-0001', 'FHZY-S-01DTU01', '演示厂商', 'DTU-2000', '1.0.0', 'CONFIRMED', 'IN_USE', 0),
  (1, 'DTU-FHZY-G-01', 2, 'DTU', 'SN-FHZY-G-01-0001', 'FHZY-G-01DTU01', '演示厂商', 'DTU-2000', '1.0.0', 'CONFIRMED', 'IN_USE', 0),
  (1, 'DTU-GMZT-G-01', 3, 'DTU', 'SN-GMZT-G-01-0001', 'GMZT-G-01DTU01', '演示厂商', 'DTU-2000', '1.0.0', 'CONFIRMED', 'IN_USE', 0);
