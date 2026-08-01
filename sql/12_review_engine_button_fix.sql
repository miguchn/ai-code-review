-- ----------------------------
-- 审查引擎按钮 menu_id 冲突修复脚本
-- 日期: 2026-08-01
-- 前置: 04_github_project_access.sql、07_review_engine.sql
-- 说明: 07_review_engine.sql 原将引擎按钮分配在 menu_id 1130-1132，与 04 的凭据按钮（1130-1134）主键冲突，
--       导致：1) 引擎按钮 INSERT 被幂等守护静默跳过，审查引擎页面无按钮权限；
--             2) sys_role_menu 误为角色 2 插入 (1130-1132)，实际授予了凭据查询/新增/修改权限。
--       本脚本：1) 仅在冲突状态下清除误授权行；2) 引擎按钮改用空闲号段 1136-1138；3) 按 07 原意图重新授权角色 2。
--       全脚本幂等，可重复执行；未受冲突影响的环境执行为空操作。
-- ----------------------------

-- 1. 清除误授权：仅当引擎按钮缺失（即确实处于冲突状态）时才删除，
--    避免误删环境中后续通过管理界面正常授予的凭据按钮权限。
DELETE rm FROM sys_role_menu rm
WHERE rm.role_id = '2'
  AND rm.menu_id IN ('1130', '1131', '1132')
  AND NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.parent_id = 124 AND m.perms = 'review:engine:query');

-- 2. 引擎按钮改用空闲号段 1136-1138（1130-1134 凭据按钮、1135 任务查询按钮已占用）
INSERT INTO sys_menu SELECT 1136, '引擎查询', 124, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:engine:query',  '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1136);
INSERT INTO sys_menu SELECT 1137, '环境检测', 124, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:engine:detect', '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1137);
INSERT INTO sys_menu SELECT 1138, '测试调用', 124, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:engine:test',   '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1138);

-- 3. 按 07 原意图为角色 2 授权引擎按钮
INSERT INTO sys_role_menu SELECT '2', '1136' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1136');
INSERT INTO sys_role_menu SELECT '2', '1137' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1137');
INSERT INTO sys_role_menu SELECT '2', '1138' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1138');
