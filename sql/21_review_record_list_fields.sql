-- ----------------------------
-- M3.1 审查记录列表字段补齐
-- 日期: 2026-08-02
-- 前置: 19_review_record_experience_m3_1.sql、20_review_record_charset_fix.sql
-- 说明:
--   1) review_task 补充 changed_files（与 additions/deletions 同源，来自既有 PR 详情请求）
--   2) 校正 pr_author 列注释为「PR 发起人」
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/21_review_record_list_fields.sql
-- ----------------------------

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS upgrade_review_task_record_list_fields_m31;
DELIMITER $$
CREATE PROCEDURE upgrade_review_task_record_list_fields_m31()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'changed_files'
  ) THEN
    ALTER TABLE review_task
      ADD COLUMN changed_files int(11) DEFAULT NULL COMMENT 'PR 变更文件数' AFTER deletions;
  END IF;

  IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'pr_author'
  ) THEN
    ALTER TABLE review_task
      MODIFY COLUMN pr_author varchar(100) DEFAULT NULL COMMENT 'PR 发起人（GitHub login）';
  END IF;
END$$
DELIMITER ;
CALL upgrade_review_task_record_list_fields_m31();
DROP PROCEDURE IF EXISTS upgrade_review_task_record_list_fields_m31;
