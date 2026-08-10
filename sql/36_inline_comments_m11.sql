-- ----------------------------
-- 36_inline_comments_m11.sql
-- M11 行内评论：投递记录关联问题、项目开关与严重度门槛、四平台行内渠道字典
--
-- 部署边界：须先执行 35_review_delivery_recovery.sql。本脚本幂等可重跑。
-- ----------------------------

DROP PROCEDURE IF EXISTS upgrade_inline_comments_m11;
DELIMITER $$
CREATE PROCEDURE upgrade_inline_comments_m11()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_delivery_record' AND column_name = 'issue_id') THEN
    ALTER TABLE review_delivery_record ADD COLUMN issue_id bigint DEFAULT NULL COMMENT '行内评论关联问题ID(总结/IM为NULL)' AFTER pr_number;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'review_delivery_record' AND index_name = 'idx_delivery_issue') THEN
    ALTER TABLE review_delivery_record ADD KEY idx_delivery_issue (issue_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'inline_comment_enabled') THEN
    ALTER TABLE review_project ADD COLUMN inline_comment_enabled char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '是否启用行内评论(0启用 1停用)' AFTER push_trigger_branches;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'inline_severities') THEN
    ALTER TABLE review_project ADD COLUMN inline_severities varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CRITICAL,HIGH' COMMENT '行内评论严重度白名单,逗号分隔' AFTER inline_comment_enabled;
  END IF;
END$$
DELIMITER ;
CALL upgrade_inline_comments_m11();
DROP PROCEDURE IF EXISTS upgrade_inline_comments_m11;

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 9, 'GitHub 行内评论', 'GITHUB_PR_INLINE_COMMENT', 'review_delivery_channel', '', 'primary', 'N', '0', 'admin', SYSDATE(), 'PR 行内评论投递'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_channel' AND dict_value = 'GITHUB_PR_INLINE_COMMENT');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 10, 'GitLab 行内评论', 'GITLAB_MR_INLINE_COMMENT', 'review_delivery_channel', '', 'primary', 'N', '0', 'admin', SYSDATE(), 'MR 行内评论投递'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_channel' AND dict_value = 'GITLAB_MR_INLINE_COMMENT');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 11, 'Gitee 行内评论', 'GITEE_PR_INLINE_COMMENT', 'review_delivery_channel', '', 'primary', 'N', '0', 'admin', SYSDATE(), 'PR 行内评论投递'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_channel' AND dict_value = 'GITEE_PR_INLINE_COMMENT');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 12, 'Gitea 行内评论', 'GITEA_PR_INLINE_COMMENT', 'review_delivery_channel', '', 'primary', 'N', '0', 'admin', SYSDATE(), 'PR 行内评论投递'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_channel' AND dict_value = 'GITEA_PR_INLINE_COMMENT');

UPDATE sys_dict_data
SET list_class = 'primary',
    update_by = 'admin',
    update_time = SYSDATE()
WHERE dict_type = 'review_delivery_channel'
  AND dict_value IN ('GITHUB_PR_INLINE_COMMENT', 'GITLAB_MR_INLINE_COMMENT', 'GITEE_PR_INLINE_COMMENT', 'GITEA_PR_INLINE_COMMENT')
  AND list_class <> 'primary';
