-- ----------------------------
-- P0/M1 GitHub 项目接入增量脚本
-- 日期: 2026-08-01
-- 前置: ry_20260417.sql、quartz.sql、sys_manage_20260512.sql
-- 说明: 新增 GitHub 凭据、代码项目、一级菜单“代码审查”及功能权限；
--       业务系统作为代码仓库项目的上层归属，菜单入口从“系统管理”移动到“代码审查”。
-- ----------------------------

CREATE TABLE IF NOT EXISTS review_git_credential (
  credential_id        bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '凭据ID',
  credential_name      varchar(64)   NOT NULL COMMENT '凭据名称',
  provider             varchar(20)   NOT NULL DEFAULT 'GITHUB' COMMENT 'Git Provider',
  auth_type            varchar(20)   NOT NULL DEFAULT 'PAT' COMMENT '认证方式',
  token_ciphertext     varchar(1000) NOT NULL COMMENT 'AES-GCM 加密后的 Token',
  status               char(1)       NOT NULL DEFAULT '0' COMMENT '状态(0启用 1停用)',
  last_check_status    varchar(20)   NOT NULL DEFAULT 'UNTESTED' COMMENT '最近检测状态',
  last_check_message   varchar(255)  DEFAULT NULL COMMENT '最近检测结果',
  last_check_time      datetime      DEFAULT NULL COMMENT '最近检测时间',
  remark               varchar(500)  DEFAULT '',
  create_by            varchar(64)   DEFAULT '',
  create_time          datetime      DEFAULT NULL,
  update_by            varchar(64)   DEFAULT '',
  update_time          datetime      DEFAULT NULL,
  PRIMARY KEY (credential_id),
  UNIQUE KEY uk_review_credential_provider_name (provider, credential_name),
  KEY idx_review_credential_status (status),
  KEY idx_review_credential_check_status (last_check_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Git 访问凭据表';

CREATE TABLE IF NOT EXISTS review_project (
  project_id           bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  project_name         varchar(100)  NOT NULL COMMENT '项目名称',
  provider             varchar(20)   NOT NULL DEFAULT 'GITHUB' COMMENT 'Git Provider',
  repository_url       varchar(500)  NOT NULL COMMENT '规范化仓库地址',
  repository_owner     varchar(100)  NOT NULL COMMENT '仓库所有者',
  repository_name      varchar(100)  NOT NULL COMMENT '仓库名称',
  default_branch       varchar(255)  DEFAULT NULL COMMENT '默认分支',
  review_branches      varchar(500)  NOT NULL DEFAULT '*' COMMENT '审查分支范围，逗号分隔',
  business_system_id   bigint(20)    NOT NULL COMMENT '业务系统ID',
  dept_id              bigint(20)    NOT NULL COMMENT '所属部门ID',
  owner_user_id        bigint(20)    NOT NULL COMMENT '项目负责人用户ID',
  credential_id        bigint(20)    NOT NULL COMMENT 'Git 凭据ID',
  status               char(1)       NOT NULL DEFAULT '1' COMMENT '状态(0启用 1停用)',
  last_check_status    varchar(20)   NOT NULL DEFAULT 'UNTESTED' COMMENT '最近检测状态',
  last_check_message   varchar(255)  DEFAULT NULL COMMENT '最近检测结果',
  last_check_time      datetime      DEFAULT NULL COMMENT '最近检测时间',
  remark               varchar(500)  DEFAULT '',
  create_by            varchar(64)   DEFAULT '',
  create_time          datetime      DEFAULT NULL,
  update_by            varchar(64)   DEFAULT '',
  update_time          datetime      DEFAULT NULL,
  PRIMARY KEY (project_id),
  UNIQUE KEY uk_review_project_repository (provider, repository_owner, repository_name),
  KEY idx_review_project_system (business_system_id),
  KEY idx_review_project_dept (dept_id),
  KEY idx_review_project_owner (owner_user_id),
  KEY idx_review_project_credential (credential_id),
  KEY idx_review_project_status (status),
  KEY idx_review_project_check_status (last_check_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代码审查项目表';

-- 一级目录“代码审查”、本切片的两个新页面，以及复用的业务系统入口。
INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 3, '代码审查', 0, 1, 'review', NULL, '', '',
       1, 0, 'M', '0', '0', '', 'code',
       'admin', SYSDATE(), '', NULL, '代码审查一级目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3);

-- 核心业务优先，平台治理与监控入口靠后。仅调整一级菜单排序，不改变其功能和权限。
UPDATE sys_menu SET order_num = 1, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 3;
UPDATE sys_menu SET order_num = 90, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 1;
UPDATE sys_menu SET order_num = 91, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 2;

-- 一个业务系统可关联多个代码仓库项目。保留 acr-system 中的治理数据和接口，
-- 只移动唯一菜单入口；原有按钮权限仍挂在 menu_id=121 下，无需复制。
UPDATE sys_menu
SET parent_id = 3,
    order_num = 1,
    path = 'business-system',
    component = 'system/businesssystem/index',
    route_name = 'BusinessSystem',
    update_by = 'admin',
    update_time = SYSDATE(),
    remark = '代码审查业务系统上层归属'
WHERE menu_id = 121;

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 122, '项目管理', 3, 2, 'project', 'review/project/index', '', 'ReviewProject',
       1, 0, 'C', '0', '0', 'review:project:list', 'github',
       'admin', SYSDATE(), '', NULL, 'GitHub 代码项目管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 122);

UPDATE sys_menu SET parent_id = 3, order_num = 2, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 122;

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 123, '访问凭据', 3, 3, 'credential', 'review/credential/index', '', 'ReviewCredential',
       1, 0, 'C', '0', '0', 'review:credential:list', 'password',
       'admin', SYSDATE(), '', NULL, 'GitHub 访问凭据管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 123);

UPDATE sys_menu SET parent_id = 3, order_num = 3, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 123;

-- 项目权限：列表、详情、新增、修改、删除、启停、连接测试。
INSERT INTO sys_menu SELECT 1124, '项目查询', 122, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:project:query', '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1124);
INSERT INTO sys_menu SELECT 1125, '项目新增', 122, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:project:add', '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1125);
INSERT INTO sys_menu SELECT 1126, '项目修改', 122, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:project:edit', '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1126);
INSERT INTO sys_menu SELECT 1127, '项目删除', 122, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:project:remove', '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1127);
INSERT INTO sys_menu SELECT 1128, '项目启停', 122, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:project:status', '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1128);
INSERT INTO sys_menu SELECT 1129, '项目连接测试', 122, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:project:test', '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1129);

-- 凭据权限：列表、详情、新增、修改、删除、连接测试。
INSERT INTO sys_menu SELECT 1130, '凭据查询', 123, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:credential:query', '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1130);
INSERT INTO sys_menu SELECT 1131, '凭据新增', 123, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:credential:add', '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1131);
INSERT INTO sys_menu SELECT 1132, '凭据修改', 123, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:credential:edit', '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1132);
INSERT INTO sys_menu SELECT 1133, '凭据删除', 123, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:credential:remove', '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1133);
INSERT INTO sys_menu SELECT 1134, '凭据连接测试', 123, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:credential:test', '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1134);

-- 超级管理员按现有权限机制自动拥有全部菜单；普通角色由管理员按职责显式授权。
