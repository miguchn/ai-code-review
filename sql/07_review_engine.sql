-- ----------------------------
-- 审查引擎（open-code-review CLI）增量脚本
-- 日期: 2026-08-01
-- 前置: 01_core_schema.sql、02_quartz_schema.sql、03_system_management.sql、06_llm_model_service.sql
-- 说明: 新增审查引擎页面及权限（模型服务菜单见 06_llm_model_service.sql）。
-- ----------------------------

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 124, '审查引擎', 4, 2, 'engine', 'review/engine/index', '', 'ReviewEngine',
       1, 0, 'C', '0', '0', 'review:engine:query', 'code',
       'admin', SYSDATE(), '', NULL, '本地 open-code-review 审查引擎'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 124);

INSERT INTO sys_menu SELECT 1130, '引擎查询', 124, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:engine:query',  '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1130);
INSERT INTO sys_menu SELECT 1131, '环境检测', 124, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:engine:detect', '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1131);
INSERT INTO sys_menu SELECT 1132, '测试调用', 124, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:engine:test',   '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1132);

INSERT INTO sys_role_menu SELECT '2', '124' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '124');
INSERT INTO sys_role_menu SELECT '2', '1130' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1130');
INSERT INTO sys_role_menu SELECT '2', '1131' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1131');
INSERT INTO sys_role_menu SELECT '2', '1132' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1132');
