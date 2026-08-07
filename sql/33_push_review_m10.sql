-- ----------------------------
-- 33_push_review_m10.sql
-- M10 审查配置与推送审查（定稿见 docs/planning/push-review-m10.md）
--
-- 范围：
--   1. review_project：推送审查开关与触发分支
--   2. review_task：事件来源列（PR/PUSH）
--   3. review_issue：参考分支维度（ref_branch），唯一键与 family 索引重建
--   4. 参数：各平台 push 事件白名单
--   5. 字典：审查事件来源 review_event_source
--
-- 兼容性：
--   - push_review_enabled 默认 '1' 停用，存量项目推送事件仍走 IGNORED，行为不变；
--   - ref_branch 默认 ''，PR 线存量行在新唯一键下唯一性不变；
--   - event_source 默认 'PR'，存量任务语义不变。
-- ----------------------------

-- 1. review_project：推送审查开关与触发分支
ALTER TABLE review_project
  ADD COLUMN push_review_enabled char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '是否启用推送审查(0启用 1停用)' AFTER pr_target_branches,
  ADD COLUMN push_trigger_branches varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '推送触发分支，换行或逗号分隔，支持通配 release/*' AFTER push_review_enabled;

-- 2. review_task：事件来源
ALTER TABLE review_task
  ADD COLUMN event_source varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PR' COMMENT '事件来源(PR=合并请求 PUSH=推送)' AFTER provider;

-- 3. review_issue：参考分支维度（PR 线空串；push 线=推送分支，任务 pr_number 哨兵 0）
ALTER TABLE review_issue
  ADD COLUMN ref_branch varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '参考分支(PR线空串；PUSH线=推送分支)' AFTER pr_number;

ALTER TABLE review_issue DROP INDEX uk_issue_pr_fingerprint;
ALTER TABLE review_issue ADD UNIQUE KEY uk_issue_ref_fingerprint (project_id, pr_number, ref_branch, fingerprint);

ALTER TABLE review_issue DROP INDEX idx_issue_pr_family;
ALTER TABLE review_issue ADD KEY idx_issue_ref_family (project_id, pr_number, ref_branch, family_key);

-- 4. 参数：各平台 push 事件白名单（对齐 prEvents/mrEvents 风格）
INSERT INTO sys_config
  (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '代码审查-GitHub默认Push事件', 'review.github.pushEvents', 'push', 'Y',
       'admin', SYSDATE(), '', NULL, '推送审查启用的 Push 触发事件'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.github.pushEvents');

INSERT INTO sys_config
  (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '代码审查-GitLab默认Push事件', 'review.gitlab.pushEvents', 'Push Hook', 'Y',
       'admin', SYSDATE(), '', NULL, '推送审查启用的 Push 触发事件'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.gitlab.pushEvents');

INSERT INTO sys_config
  (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '代码审查-Gitee默认Push事件', 'review.gitee.pushEvents', 'Push Hook', 'Y',
       'admin', SYSDATE(), '', NULL, '推送审查启用的 Push 触发事件'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.gitee.pushEvents');

INSERT INTO sys_config
  (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '代码审查-Gitea默认Push事件', 'review.gitea.pushEvents', 'push', 'Y',
       'admin', SYSDATE(), '', NULL, '推送审查启用的 Push 触发事件'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.gitea.pushEvents');

-- 5. 字典：审查事件来源
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审查事件来源', 'review_event_source', '0', 'admin', SYSDATE(), '审查任务触发事件来源（合并请求/推送）'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_event_source');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '合并请求', 'PR', 'review_event_source', '', 'primary', 'Y', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_event_source' AND dict_value = 'PR');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '推送', 'PUSH', 'review_event_source', '', 'success', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_event_source' AND dict_value = 'PUSH');
