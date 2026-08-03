-- ----------------------------
-- M6.1：问题处置与投递记录追溯打磨
-- 日期: 2026-08-03
-- 前置: 23_review_delivery_record.sql、25_issue_ledger_m6.sql
-- 说明:
--   1) review_delivery_record 增 trigger_source（可空，历史不回填）
--   2) 字典 review_delivery_trigger_source：TASK_SUCCESS / ISSUE_DISPOSITION / MANUAL_RETRY
--   3) 不加菜单、不加权限
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/26_issue_delivery_trace_m6_1.sql
-- ----------------------------

SET NAMES utf8mb4;

-- ----------------------------
-- 投递触发来源列（只加不改）
-- ----------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_delivery_record' AND column_name = 'trigger_source'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE review_delivery_record ADD COLUMN trigger_source varchar(32) DEFAULT NULL COMMENT ''触发来源(TASK_SUCCESS/ISSUE_DISPOSITION/MANUAL_RETRY)'' AFTER last_attempt_time',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 字典：投递触发来源
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审查投递触发来源', 'review_delivery_trigger_source', '0', 'admin', SYSDATE(), '投递记录最近一次尝试的触发来源'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_delivery_trigger_source');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '任务回写', 'TASK_SUCCESS', 'review_delivery_trigger_source', '', 'primary', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_trigger_source' AND dict_value = 'TASK_SUCCESS');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '问题处置', 'ISSUE_DISPOSITION', 'review_delivery_trigger_source', '', 'warning', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_trigger_source' AND dict_value = 'ISSUE_DISPOSITION');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '手动重试', 'MANUAL_RETRY', 'review_delivery_trigger_source', '', 'info', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_trigger_source' AND dict_value = 'MANUAL_RETRY');
