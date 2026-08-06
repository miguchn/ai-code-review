-- ----------------------------
-- M8.1：PR 关闭/合并联动关闭来源字典
-- 日期: 2026-08-06
-- 前置: 25_issue_ledger_m6.sql
-- 说明:
--   1) review_issue_close_source 增 pr_merged / pr_closed 两行
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/32_issue_pr_close_source.sql
-- ----------------------------

SET NAMES utf8mb4;

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '随 PR 合并关闭', 'pr_merged', 'review_issue_close_source', '', 'success', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_issue_close_source' AND dict_value = 'pr_merged');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '随 PR 关闭', 'pr_closed', 'review_issue_close_source', '', 'info', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_issue_close_source' AND dict_value = 'pr_closed');
