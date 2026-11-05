-- V10: 数据权限 REGION 作用域依赖的用户区域绑定
-- 监管用户（REG）绑定 region_id 后，站房/报警/任务列表按区域收紧；
-- 未绑定的监管用户维持原行为（租户内全量），避免升级即断权限。
ALTER TABLE sys_user
  ADD COLUMN region_id BIGINT NULL COMMENT '所属区域，REGION 数据权限用' AFTER org_id;
