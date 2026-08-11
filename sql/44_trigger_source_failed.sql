-- ----------------------------
-- 44_trigger_source_failed.sql
-- 投递触发来源补 TASK_FAILED（失败简讯）
--
-- 前置: 26_issue_delivery_trace_m6_1.sql
-- 说明:
--   1) 字典 review_delivery_trigger_source 增 TASK_FAILED /「任务失败通知」
--   2) review_delivery_record.trigger_source 列注释纳入 TASK_FAILED（仅注释）
--   3) 历史数据不回填
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/44_trigger_source_failed.sql
-- ----------------------------

SET NAMES utf8mb4;

-- ----------------------------
-- 字典：任务失败通知
-- ----------------------------
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '任务失败通知', 'TASK_FAILED', 'review_delivery_trigger_source', '', 'warning', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_trigger_source' AND dict_value = 'TASK_FAILED');

-- ----------------------------
-- 列注释纳入 TASK_FAILED（仅注释，幂等）
-- ----------------------------
SET @comment_needs_update := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE()
    AND table_name = 'review_delivery_record'
    AND column_name = 'trigger_source'
    AND (COLUMN_COMMENT IS NULL OR COLUMN_COMMENT NOT LIKE '%TASK_FAILED%')
);
SET @sql := IF(@comment_needs_update > 0,
  'ALTER TABLE review_delivery_record MODIFY COLUMN trigger_source varchar(32) DEFAULT NULL COMMENT ''触发来源(TASK_SUCCESS/TASK_FAILED/ISSUE_DISPOSITION/MANUAL_RETRY)''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
