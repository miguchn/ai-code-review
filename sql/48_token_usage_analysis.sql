-- ----------------------------
-- 48_token_usage_analysis.sql
-- Token 用量分析：run 级用量列、模型单价、查询索引、数据洞察二级菜单
--
-- 前置: 47_production_readiness_governance.sql
-- 部署边界：幂等可重跑。须 utf8mb4 连接执行。不建聚合表。
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/48_token_usage_analysis.sql
-- ----------------------------

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS upgrade_token_usage_analysis;
DELIMITER $$
CREATE PROCEDURE upgrade_token_usage_analysis()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'review_task_run'
      AND column_name = 'input_tokens'
  ) THEN
    ALTER TABLE review_task_run
      ADD COLUMN input_tokens int NULL COMMENT '输入 token（未采集为 NULL）' AFTER duration_ms;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'review_task_run'
      AND column_name = 'output_tokens'
  ) THEN
    ALTER TABLE review_task_run
      ADD COLUMN output_tokens int NULL COMMENT '输出 token（未采集为 NULL）' AFTER input_tokens;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'review_task_run'
      AND column_name = 'total_tokens'
  ) THEN
    ALTER TABLE review_task_run
      ADD COLUMN total_tokens int NULL COMMENT '总 token（未采集为 NULL）' AFTER output_tokens;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_ai_model_config'
      AND column_name = 'input_price_per_1k'
  ) THEN
    ALTER TABLE sys_ai_model_config
      ADD COLUMN input_price_per_1k decimal(10,4) NULL COMMENT '输入单价（元/千 token）' AFTER context_length;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_ai_model_config'
      AND column_name = 'output_price_per_1k'
  ) THEN
    ALTER TABLE sys_ai_model_config
      ADD COLUMN output_price_per_1k decimal(10,4) NULL COMMENT '输出单价（元/千 token）' AFTER input_price_per_1k;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'review_task_run'
      AND index_name = 'idx_run_token_time'
  ) THEN
    ALTER TABLE review_task_run
      ADD INDEX idx_run_token_time (create_time, snapshot_model_id);
  END IF;
END$$
DELIMITER ;

CALL upgrade_token_usage_analysis();
DROP PROCEDURE IF EXISTS upgrade_token_usage_analysis;

-- 菜单：数据洞察 / Token 用量分析（报告中心顺序后移）
INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 138, 'Token 用量分析', 7, 4, 'token', 'insight/token/index', '', 'InsightTokenUsage',
       1, 0, 'C', '0', '0', 'insight:token:view', 'money',
       'admin', SYSDATE(), '', NULL, '审查 Token 用量与估算成本观察'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 138 OR perms = 'insight:token:view');

UPDATE sys_menu SET order_num = 5, update_by = 'admin', update_time = SYSDATE()
WHERE menu_id = 134 AND component = 'insight/report/placeholder';

UPDATE sys_menu
SET remark = '数据洞察：总览看板、项目分析、成员分析、Token 用量分析',
    update_by = 'admin',
    update_time = SYSDATE()
WHERE menu_id = 7;

INSERT INTO sys_menu
SELECT 1177, 'Token 用量查看', 138, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'insight:token:view', '#',
       'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1177);

-- 角色 2/4/5/6 授权（判重）；超级管理员 role_id=1 默认全量
INSERT INTO sys_role_menu SELECT '2', '138' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '138');
INSERT INTO sys_role_menu SELECT '2', '1177' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1177');
INSERT INTO sys_role_menu SELECT '4', '138' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '4' AND menu_id = '138');
INSERT INTO sys_role_menu SELECT '4', '1177' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '4' AND menu_id = '1177');
INSERT INTO sys_role_menu SELECT '5', '138' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '5' AND menu_id = '138');
INSERT INTO sys_role_menu SELECT '5', '1177' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '5' AND menu_id = '1177');
INSERT INTO sys_role_menu SELECT '6', '138' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '6' AND menu_id = '138');
INSERT INTO sys_role_menu SELECT '6', '1177' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '6' AND menu_id = '1177');
