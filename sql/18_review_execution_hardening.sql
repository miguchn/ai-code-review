-- ----------------------------
-- M3 审查执行加固脚本
-- 日期: 2026-08-02
-- 前置: 13_review_pipeline_m3.sql、15_review_template_config.sql、16_review_scoring_result_protocol.sql
-- 说明: 1) review_task_run.snapshot_review_mode 放宽为可空：run 建立在 RESOLVE_CONFIG 之前，
--          历史任务（无建单快照）执行时由执行服务落 FAILED 而非插入失败卡死；
--       2) 失败类型字典补充 RATE_LIMIT（GitHub API 限流）；
--       3) 下线未使用的「模板复制」按钮（前端复制走新增接口，权限统一为 review:template:add）。
--       全脚本幂等，可重复执行。
-- ----------------------------

-- 1. run 快照审查方式允许为空（执行服务在解析配置后回填）
ALTER TABLE review_task_run
    MODIFY COLUMN snapshot_review_mode varchar(40) NULL COMMENT '快照：审查方式';

-- 2. 失败类型字典：API 限流
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 10, 'API 限流', 'RATE_LIMIT', 'review_task_failure_type', '', 'warning', 'N', '0', 'admin', SYSDATE(), 'GitHub API 触发限流，稍后重试'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_failure_type' AND dict_value = 'RATE_LIMIT');

-- 3. 下线「模板复制」按钮及授权（复制功能复用新增接口与 review:template:add 权限）
DELETE FROM sys_role_menu WHERE menu_id = '1148';
DELETE FROM sys_menu WHERE menu_id = 1148 AND perms = 'review:template:copy';
