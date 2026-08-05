-- ----------------------------
-- 投递正文快照：记录实际发出的 IM / 总结评论内容
-- 日期: 2026-08-05
-- 前置: 23_review_delivery_record.sql、26_issue_delivery_trace_m6_1.sql
-- 说明:
--   1) review_delivery_record 增 content_snapshot（mediumtext，可空，历史不回填）
--   2) JSON: {"kind":"IM"|"SUMMARY_COMMENT","channelType":...,"title":...,"body":...}
--   3) 不加菜单、不加权限
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/30_delivery_content_snapshot.sql
-- ----------------------------

SET NAMES utf8mb4;

-- ----------------------------
-- 投递正文快照列（只加不改）
-- ----------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_delivery_record' AND column_name = 'content_snapshot'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE review_delivery_record ADD COLUMN content_snapshot mediumtext DEFAULT NULL COMMENT ''实际发出正文快照(JSON)'' AFTER trigger_source',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
