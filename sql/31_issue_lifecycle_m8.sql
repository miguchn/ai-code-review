-- ----------------------------
-- M8：问题生命周期管理（Stage 1）
-- 日期: 2026-08-05
-- 前置: 25_issue_ledger_m6.sql
-- 说明:
--   1) review_issue 增对账/复核证据列与 family 索引
--   2) 字典：RECHECKING 改「待复核」并去预留备注；auto_recheck 去预留备注
--   3) 参数：连续未命中阈值、协议问题清单上限
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/31_issue_lifecycle_m8.sql
-- ----------------------------

SET NAMES utf8mb4;

-- ----------------------------
-- 1. review_issue 对账列（只加不改）
-- ----------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_issue' AND column_name = 'family_key'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE review_issue ADD COLUMN family_key varchar(80) DEFAULT NULL COMMENT ''族键 SHA-256(filePath+NUL+category)'' AFTER fingerprint',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_issue' AND column_name = 'missed_streak'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE review_issue ADD COLUMN missed_streak int(11) NOT NULL DEFAULT 0 COMMENT ''连续未命中轮数'' AFTER closed_time',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_issue' AND column_name = 'last_seen_head_sha'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE review_issue ADD COLUMN last_seen_head_sha varchar(64) DEFAULT NULL COMMENT ''最近命中轮 head commit'' AFTER missed_streak',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_issue' AND column_name = 'last_missed_run_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE review_issue ADD COLUMN last_missed_run_id bigint(20) DEFAULT NULL COMMENT ''最近计未命中的 run（对账幂等键）'' AFTER last_seen_head_sha',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_issue' AND column_name = 'recheck_task_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE review_issue ADD COLUMN recheck_task_id bigint(20) DEFAULT NULL COMMENT ''触发待复核的任务ID'' AFTER last_missed_run_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_issue' AND column_name = 'recheck_run_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE review_issue ADD COLUMN recheck_run_id bigint(20) DEFAULT NULL COMMENT ''触发待复核的 runID'' AFTER recheck_task_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_issue' AND column_name = 'recheck_commit_sha'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE review_issue ADD COLUMN recheck_commit_sha varchar(64) DEFAULT NULL COMMENT ''未命中轮 head commit'' AFTER recheck_run_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE table_schema = DATABASE() AND table_name = 'review_issue' AND index_name = 'idx_issue_pr_family'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE review_issue ADD KEY idx_issue_pr_family (project_id, pr_number, family_key)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 2. 字典：启用待复核 / 自动复核关闭来源
-- ----------------------------
UPDATE sys_dict_data
SET dict_label = '待复核',
    remark = '',
    update_by = 'admin',
    update_time = SYSDATE()
WHERE dict_type = 'review_issue_status'
  AND dict_value = 'RECHECKING';

UPDATE sys_dict_data
SET remark = '',
    update_by = 'admin',
    update_time = SYSDATE()
WHERE dict_type = 'review_issue_close_source'
  AND dict_value = 'auto_recheck';

-- ----------------------------
-- 3. 运行参数
-- ----------------------------
INSERT INTO sys_config
  (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '问题台账-转待复核未命中轮数阈值', 'review.issue.recheck.missedRoundsThreshold', '1', 'Y', 'admin', SYSDATE(), '', NULL,
       '连续未命中 N 轮后自动转待复核；关闭仍须人工确认'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.issue.recheck.missedRoundsThreshold');

INSERT INTO sys_config
  (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '审查协议-单轮问题清单上限', 'review.protocol.maxIssues', '20', 'Y', 'admin', SYSDATE(), '', NULL,
       '协议 v1.2 解析截断上限；超出记截断标记'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.protocol.maxIssues');
