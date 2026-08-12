-- ----------------------------
-- 45_project_permission_governance.sql
-- 项目成员隔离与平台敏感资产角色边界
--
-- 前置: 44_trigger_source_failed.sql
-- 说明:
--   1) sys_role 增 role_scope，存量角色安全回落为 DEPARTMENT，超级管理员为 PLATFORM
--   2) review_project_member 增项目显式成员与项目角色
--   3) 项目负责人通过 review_project.owner_user_id 固定拥有 OWNER，不重复写成员表
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/45_project_permission_governance.sql
-- ----------------------------

SET NAMES utf8mb4;

SET @role_scope_missing := (
  SELECT COUNT(1) = 0
  FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_role'
    AND column_name = 'role_scope'
);
SET @sql := IF(@role_scope_missing,
  'ALTER TABLE sys_role ADD COLUMN role_scope varchar(20) NOT NULL DEFAULT ''DEPARTMENT'' COMMENT ''角色层级(PLATFORM平台级 DEPARTMENT部门级)'' AFTER data_scope',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE sys_role
SET role_scope = 'DEPARTMENT'
WHERE role_scope IS NULL OR role_scope NOT IN ('PLATFORM', 'DEPARTMENT');

UPDATE sys_role
SET role_scope = 'PLATFORM'
WHERE role_id = 1;

CREATE TABLE IF NOT EXISTS review_project_member (
  member_id bigint NOT NULL AUTO_INCREMENT COMMENT '成员授权ID',
  project_id bigint NOT NULL COMMENT '项目ID',
  user_id bigint NOT NULL COMMENT '用户ID',
  project_role varchar(20) NOT NULL COMMENT '项目角色(ADMIN/REVIEWER/VIEWER)',
  status char(1) NOT NULL DEFAULT '0' COMMENT '状态(0正常 1停用)',
  create_by varchar(64) DEFAULT '' COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT '' COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (member_id),
  UNIQUE KEY uk_project_member (project_id, user_id),
  KEY idx_project_member_user (user_id, status),
  KEY idx_project_member_project (project_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代码审查项目成员授权';
