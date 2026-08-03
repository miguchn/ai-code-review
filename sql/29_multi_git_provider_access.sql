-- ----------------------------
-- 多 Git Provider 接入：数据层增量（S1）
-- 日期: 2026-08-03
-- 前置: 04_github_project_access.sql、08_github_pr_webhook.sql、24_notification_management_m5.sql
-- 说明:
--   1) review_git_credential 增 server_url（GitLab/Gitea 自建实例 Web 根地址）
--   2) review_project 增 repository_full_path、唯一键迁移、owner/name 放宽至 255
--   3) review_webhook_event 增 repository_full_path、owner/name 放宽至 255
--   4) 字典 review_git_provider；扩展 review_delivery_channel
--   5) 参数 review.gitlab.mrEvents / review.gitee.prEvents / review.gitea.prEvents
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/29_multi_git_provider_access.sql
-- ----------------------------

SET NAMES utf8mb4;

-- ----------------------------
-- review_git_credential：服务地址
-- ----------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_git_credential' AND column_name = 'server_url'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE review_git_credential ADD COLUMN server_url varchar(500) DEFAULT NULL COMMENT ''Git Web 根地址（GitLab/Gitea 必填；GitHub/Gitee 须为空）'' AFTER auth_type',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- review_project：repository_full_path 迁移
-- ----------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'repository_full_path'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE review_project ADD COLUMN repository_full_path varchar(255) DEFAULT NULL COMMENT ''仓库全路径身份'' AFTER repository_name',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE review_project
SET repository_full_path = CONCAT(repository_owner, '/', repository_name)
WHERE repository_full_path IS NULL
  AND repository_owner IS NOT NULL
  AND repository_name IS NOT NULL;

SET @is_nullable := (
  SELECT IS_NULLABLE FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'repository_full_path'
);
SET @sql := IF(@is_nullable = 'YES',
  'ALTER TABLE review_project MODIFY COLUMN repository_full_path varchar(255) NOT NULL COMMENT ''仓库全路径身份''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE table_schema = DATABASE() AND table_name = 'review_project' AND index_name = 'uk_review_project_full_path'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE review_project ADD UNIQUE KEY uk_review_project_full_path (provider, repository_full_path)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE table_schema = DATABASE() AND table_name = 'review_project' AND index_name = 'uk_review_project_repository'
);
SET @sql := IF(@idx_exists > 0,
  'ALTER TABLE review_project DROP INDEX uk_review_project_repository',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_len := (
  SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'repository_owner'
);
SET @sql := IF(@col_len IS NOT NULL AND @col_len < 255,
  'ALTER TABLE review_project MODIFY COLUMN repository_owner varchar(255) NOT NULL COMMENT ''仓库所有者（命名空间前缀）''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_len := (
  SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'repository_name'
);
SET @sql := IF(@col_len IS NOT NULL AND @col_len < 255,
  'ALTER TABLE review_project MODIFY COLUMN repository_name varchar(255) NOT NULL COMMENT ''仓库名称''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- review_webhook_event：repository_full_path 与列宽
-- ----------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_webhook_event' AND column_name = 'repository_full_path'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE review_webhook_event ADD COLUMN repository_full_path varchar(255) DEFAULT NULL COMMENT ''仓库全路径（审计）'' AFTER repository_name',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_len := (
  SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_webhook_event' AND column_name = 'repository_owner'
);
SET @sql := IF(@col_len IS NOT NULL AND @col_len < 255,
  'ALTER TABLE review_webhook_event MODIFY COLUMN repository_owner varchar(255) DEFAULT NULL COMMENT ''仓库所有者''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_len := (
  SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_webhook_event' AND column_name = 'repository_name'
);
SET @sql := IF(@col_len IS NOT NULL AND @col_len < 255,
  'ALTER TABLE review_webhook_event MODIFY COLUMN repository_name varchar(255) DEFAULT NULL COMMENT ''仓库名称''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 字典：Git Provider 平台
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT 'Git 平台', 'review_git_provider', '0', 'admin', SYSDATE(), '代码审查支持的 Git 托管平台'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_git_provider');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, 'GitHub', 'GITHUB', 'review_git_provider', '', 'info', 'Y', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_git_provider' AND dict_value = 'GITHUB');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, 'GitLab', 'GITLAB', 'review_git_provider', '', 'primary', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_git_provider' AND dict_value = 'GITLAB');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, 'Gitee（码云）', 'GITEE', 'review_git_provider', '', 'success', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_git_provider' AND dict_value = 'GITEE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, 'Gitea', 'GITEA', 'review_git_provider', '', 'warning', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_git_provider' AND dict_value = 'GITEA');

-- ----------------------------
-- 字典：投递渠道（GitLab / Gitee / Gitea 总结评论）
-- ----------------------------
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, 'GitLab 总结评论', 'GITLAB_MR_SUMMARY_COMMENT', 'review_delivery_channel', '', 'primary', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_channel' AND dict_value = 'GITLAB_MR_SUMMARY_COMMENT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, 'Gitee 总结评论', 'GITEE_PR_SUMMARY_COMMENT', 'review_delivery_channel', '', 'success', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_channel' AND dict_value = 'GITEE_PR_SUMMARY_COMMENT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 7, 'Gitea 总结评论', 'GITEA_PR_SUMMARY_COMMENT', 'review_delivery_channel', '', 'warning', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_channel' AND dict_value = 'GITEA_PR_SUMMARY_COMMENT');

-- ----------------------------
-- 参数：各平台合并请求事件白名单
-- ----------------------------
INSERT INTO sys_config
  (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '代码审查-GitLab默认MR事件', 'review.gitlab.mrEvents', 'opened,reopened,synchronize', 'Y',
       'admin', SYSDATE(), '', NULL, '统一启用的 Merge Request 触发事件'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.gitlab.mrEvents');

INSERT INTO sys_config
  (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '代码审查-Gitee默认PR事件', 'review.gitee.prEvents', 'opened,reopened,synchronize', 'Y',
       'admin', SYSDATE(), '', NULL, '统一启用的 Pull Request 触发事件'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.gitee.prEvents');

INSERT INTO sys_config
  (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '代码审查-Gitea默认PR事件', 'review.gitea.prEvents', 'opened,reopened,synchronize', 'Y',
       'admin', SYSDATE(), '', NULL, '统一启用的 Pull Request 触发事件'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.gitea.prEvents');
