-- ============================================================
-- V2 演示数据：租户 / 角色 / 管理员 / 企业 / 站房 / 因子
-- 管理员：admin / Admin@123（BCrypt）
-- ============================================================

INSERT INTO sys_tenant (tenant_code, name, tenant_type, status, deleted)
VALUES ('DEMO', '成都市演示监管域', 'REG', 1, 0);

SET @tenant_id = LAST_INSERT_ID();

INSERT INTO sys_role (tenant_id, role_code, name, data_scope, deleted) VALUES
  (@tenant_id, 'SUP', '平台超级管理员', 'ALL', 0),
  (@tenant_id, 'REG', '监管用户', 'REGION', 0),
  (@tenant_id, 'ORG', '运维单位管理员', 'ORG', 0),
  (@tenant_id, 'MGR', '运维主管', 'ORG', 0),
  (@tenant_id, 'ENG', '运维工程师', 'SELF', 0),
  (@tenant_id, 'ENT', '企业用户', 'ORG', 0);

INSERT INTO sys_user (tenant_id, username, password_hash, real_name, status, deleted)
VALUES (@tenant_id, 'admin',
        '$2a$10$zRPXy.ujEA1Upnagk4pC/uYkwyrAeKf6BeovLnkCmpXHTd9bwd3z.',
        '系统管理员', 1, 0);

SET @admin_id = LAST_INSERT_ID();

INSERT INTO sys_user_role (user_id, role_id)
SELECT @admin_id, id FROM sys_role
WHERE tenant_id = @tenant_id AND role_code IN ('SUP', 'MGR');

INSERT INTO md_enterprise (tenant_id, credit_code, name, short_name, industry_code,
                           region_id, address, is_key_unit, status, deleted) VALUES
  (@tenant_id, '91510100MA6C0DEMO1', '丰华纸业有限公司', '丰华纸业', 'C22', 1, '四川省成都市演示路 1 号', 1, 1, 0),
  (@tenant_id, '91510100MA6C0DEMO2', '光明玻璃制品有限公司', '光明玻特', 'C30', 1, '四川省成都市演示路 2 号', 1, 1, 0);

INSERT INTO md_station (tenant_id, station_code, name, station_type, enterprise_id, region_id,
                        lng, lat, address, biz_status, is_focus, deleted) VALUES
  (@tenant_id, 'FHZY-S-01', '丰华纸业-水',   'WASTE_WATER', 1, 1, 103.9301, 30.5712, '四川省成都市演示路 1 号', 'NORMAL', 0, 0),
  (@tenant_id, 'FHZY-G-01', '丰华纸业-锅炉', 'WASTE_GAS',   1, 1, 103.9305, 30.5716, '四川省成都市演示路 1 号', 'NORMAL', 0, 0),
  (@tenant_id, 'GMZT-G-01', '光明玻特废气', 'WASTE_GAS',   2, 1, 103.9412, 30.5803, '四川省成都市演示路 2 号', 'NORMAL', 0, 0);

INSERT INTO md_station_factor (tenant_id, station_id, factor_code, unit, range_min, range_max, std_limit, is_main, deleted) VALUES
  (@tenant_id, 1, 'a21026', 'mg/L', 0, 500, 40, 1, 0),
  (@tenant_id, 1, 'a00000', 'l/s',  0, 500, NULL, 1, 0),
  (@tenant_id, 1, 'pH',     '',     0, 14,  9,    0, 0),
  (@tenant_id, 2, 'a21002', 'mg/m3', 0, 1000, 50,  1, 0),
  (@tenant_id, 2, 'a21001', 'mg/m3', 0, 1000, 100, 1, 0),
  (@tenant_id, 2, 'a21003', 'mg/m3', 0, 1000, 20,  0, 0),
  (@tenant_id, 3, 'a21002', 'mg/m3', 0, 1000, 50,  1, 0),
  (@tenant_id, 3, 'a21001', 'mg/m3', 0, 1000, 100, 1, 0);
