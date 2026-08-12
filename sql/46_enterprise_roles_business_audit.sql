-- ----------------------------
-- 46_enterprise_roles_business_audit.sql
-- 企业标准角色模板与业务审计事实
--
-- 前置: 45_project_permission_governance.sql
-- 说明:
--   1) 保留超级管理员；补齐平台管理员、开发人员、项目负责人、质量/安全人员、审计人员最小权限模板。
--   2) 修复历史脚本仅授予 role_id=2 菜单但未初始化角色记录的问题；平台管理员角色不复制超级管理员的系统治理权限。
--   3) sys_business_audit 只允许业务服务新增和查询，前后值快照独立保存，不提供业务更新/删除接口。
--   4) 问题关闭、忽略、误报由应用层强制填写原因；系统联动关闭记录关闭来源和系统说明。
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/46_enterprise_roles_business_audit.sql
-- ----------------------------

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_business_audit (
  audit_id       bigint NOT NULL AUTO_INCREMENT COMMENT '业务审计ID',
  event_key      varchar(160) NOT NULL COMMENT '事件幂等标识',
  source         varchar(32) NOT NULL COMMENT '来源模块',
  action         varchar(64) NOT NULL COMMENT '业务动作',
  object_type    varchar(64) NOT NULL COMMENT '操作对象类型',
  object_id      varchar(128) DEFAULT NULL COMMENT '操作对象ID',
  object_name    varchar(255) DEFAULT NULL COMMENT '操作对象名称',
  before_value   mediumtext COMMENT '关键前值快照（不含凭据）',
  after_value    mediumtext COMMENT '关键后值快照（不含凭据）',
  reason         varchar(1000) DEFAULT NULL COMMENT '操作原因或系统依据',
  related_object mediumtext COMMENT '关联项目、任务、运行或授权对象',
  operator       varchar(64) NOT NULL COMMENT '操作人',
  audit_time     datetime NOT NULL COMMENT '审计时间',
  PRIMARY KEY (audit_id),
  UNIQUE KEY uk_business_audit_event (event_key),
  KEY idx_business_audit_time (audit_time),
  KEY idx_business_audit_object (object_type, object_id),
  KEY idx_business_audit_operator (operator)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务审计事实（仅新增）';

-- 角色模板：role_id 固定仅用于新装及历史 role_id=2 的补全；已存在同 ID 或同 role_key 时保持存量配置。
INSERT INTO sys_role
  (role_id, role_name, role_key, role_sort, data_scope, role_scope,
   menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT 2, '审查平台管理员', 'acr_platform_admin', 2, '1', 'PLATFORM', 1, 1, '0', '0', 'admin', sysdate(),
       '平台运行与审查配置管理员；不含用户、角色、菜单等超级管理员权限'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 2 OR role_key = 'acr_platform_admin');

INSERT INTO sys_role
  (role_id, role_name, role_key, role_sort, data_scope, role_scope,
   menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT 3, '开发人员', 'acr_developer', 10, '3', 'DEPARTMENT', 1, 1, '0', '0', 'admin', sysdate(),
       '查看授权项目、任务、记录和问题；可确认及处置授权范围内问题'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 3 OR role_key = 'acr_developer');

INSERT INTO sys_role
  (role_id, role_name, role_key, role_sort, data_scope, role_scope,
   menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT 4, '项目负责人', 'acr_project_owner', 20, '2', 'DEPARTMENT', 1, 1, '0', '0', 'admin', sysdate(),
       '负责授权项目接入、任务处置和项目级审查策略使用'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 4 OR role_key = 'acr_project_owner');

INSERT INTO sys_role
  (role_id, role_name, role_key, role_sort, data_scope, role_scope,
   menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT 5, '质量/安全人员', 'acr_quality_security', 30, '2', 'PLATFORM', 1, 1, '0', '0', 'admin', sysdate(),
       '平台级维护审查模板和引擎策略；仅在加入项目后复核授权范围内高风险问题'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 5 OR role_key = 'acr_quality_security');

INSERT INTO sys_role
  (role_id, role_name, role_key, role_sort, data_scope, role_scope,
   menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT 6, '审计人员', 'acr_auditor', 40, '2', 'DEPARTMENT', 1, 1, '0', '0', 'admin', sysdate(),
       '只读查询业务审计、操作日志、审查结果和权限相关证据'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 6 OR role_key = 'acr_auditor');

-- 业务审计查询菜单，不授予修改或删除能力。
INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 137, '业务审计', 1, 10, 'audit', 'system/audit/index', '', 'BusinessAudit',
       1, 0, 'C', '0', '0', 'system:audit:list', 'document', 'admin', sysdate(),
       '问题处置、策略和授权变更的不可覆盖业务事实'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 137 OR perms = 'system:audit:list');

-- 平台管理员沿用历史 role_id=2 的审查菜单，并补充业务审计入口；不补授系统用户/角色/菜单维护权限。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, 1 FROM sys_role WHERE role_key = 'acr_platform_admin';
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, 137 FROM sys_role WHERE role_key = 'acr_platform_admin';

-- 开发人员：授权项目内的问题处理和结果查看，不含项目/凭据/策略管理。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r JOIN sys_menu m
  ON m.menu_id IN (3, 6, 122, 125, 128, 131, 1124, 1135, 1149, 1160, 1161, 1162, 1163)
WHERE r.role_key = 'acr_developer';

-- 项目负责人：项目接入、任务处置、投递追踪和项目级问题处理；不含全局模板/模型维护。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r JOIN sys_menu m
  ON m.menu_id IN (3, 5, 6, 7, 122, 125, 127, 128, 130, 131, 133,
                   1124, 1125, 1126, 1128, 1129, 1135, 1139,
                   1144, 1149, 1158, 1159, 1160, 1161, 1162, 1163, 1150, 1171)
WHERE r.role_key = 'acr_project_owner';

-- 质量/安全人员：平台级审查规则、引擎和高风险问题复核；不含系统账号和角色治理。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r JOIN sys_menu m
  ON m.menu_id IN (3, 4, 5, 6, 7, 122, 125, 127, 128, 130, 131, 132, 133, 135,
                   1124, 1135, 1144, 1145, 1146, 1147, 1149,
                   1158, 1159, 1160, 1161, 1162, 1163, 1170, 1171, 1172)
WHERE r.role_key = 'acr_quality_security';

-- 审计人员：只读审计和业务证据；不授任何新增、修改、删除、启停、处置或导出权限。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r JOIN sys_menu m
  ON m.menu_id IN (1, 2, 3, 5, 6, 7, 108, 122, 125, 128, 130, 131,
                   132, 133, 134, 135, 136, 137, 500, 501, 1039, 1042, 1124, 1135,
                   1136, 1149, 1151, 1158, 1159, 1160, 1161, 1170, 1171, 1172, 1173)
WHERE r.role_key = 'acr_auditor';

-- 审查平台管理员的历史 role_id=2 关联若存在则不覆盖；按 role_key 补审计菜单。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, 137 FROM sys_role WHERE role_key = 'acr_platform_admin';
