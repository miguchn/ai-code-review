-- =============================================================================
-- 27_sidebar_menu_ia.sql
-- 左侧菜单信息架构调整：审查中心 / 项目接入 / 策略配置；系统管理与系统监控不动。
-- 须 utf8mb4 连接执行。幂等可重复执行。
-- 设计：docs/superpowers/specs/2026-08-03-sidebar-menu-ia-design.md
-- =============================================================================

-- ----------------------------
-- 1. 新建一级目录「项目接入」
-- ----------------------------
INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 6, '项目接入', 0, 2, 'project-access', NULL, '', '',
       1, 0, 'M', '0', '0', '', 'tree',
       'admin', SYSDATE(), '', NULL, '项目接入一级目录（业务系统/凭据/代码项目）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 6);

-- ----------------------------
-- 2. 一级菜单重命名与排序
--    工作台为常量路由置顶；其后：审查中心 → 项目接入 → 策略配置 → 通知管理 → 系统管理 → 系统监控
-- ----------------------------
UPDATE sys_menu
SET menu_name = '审查中心',
    order_num = 1,
    update_by = 'admin',
    update_time = SYSDATE(),
    remark = '审查中心：问题台账、审查任务、审查记录'
WHERE menu_id = 3;

UPDATE sys_menu
SET menu_name = '项目接入',
    order_num = 2,
    update_by = 'admin',
    update_time = SYSDATE(),
    remark = '项目接入一级目录（业务系统/凭据/代码项目）'
WHERE menu_id = 6;

UPDATE sys_menu
SET menu_name = '策略配置',
    order_num = 3,
    update_by = 'admin',
    update_time = SYSDATE(),
    remark = '策略配置：审查模板、大模型配置、审查引擎'
WHERE menu_id = 4;

UPDATE sys_menu SET order_num = 4, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 5;
UPDATE sys_menu SET order_num = 5, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 1;
UPDATE sys_menu SET order_num = 6, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 2;

-- ----------------------------
-- 3. 审查中心子菜单（保留 parent_id=3，仅调整顺序）
-- ----------------------------
UPDATE sys_menu
SET parent_id = 3, order_num = 1, update_by = 'admin', update_time = SYSDATE(),
    remark = '审查问题确认与关闭'
WHERE menu_id = 131;

UPDATE sys_menu
SET parent_id = 3, order_num = 2, update_by = 'admin', update_time = SYSDATE(),
    remark = '审查执行队列：待执行/执行中/失败与重试'
WHERE menu_id = 125;

UPDATE sys_menu
SET parent_id = 3, order_num = 3, update_by = 'admin', update_time = SYSDATE(),
    remark = '已完成审查结果历史'
WHERE menu_id = 128;

-- ----------------------------
-- 4. 项目接入子菜单（迁入 menu_id=6）+ 命名统一
-- ----------------------------
UPDATE sys_menu
SET parent_id = 6,
    order_num = 1,
    menu_name = '业务系统',
    update_by = 'admin',
    update_time = SYSDATE(),
    remark = '业务系统上层归属'
WHERE menu_id = 121;

UPDATE sys_menu
SET parent_id = 6,
    order_num = 2,
    menu_name = '访问凭据',
    update_by = 'admin',
    update_time = SYSDATE(),
    remark = '代码平台访问凭据'
WHERE menu_id = 123;

UPDATE sys_menu
SET parent_id = 6,
    order_num = 3,
    menu_name = '代码项目',
    update_by = 'admin',
    update_time = SYSDATE(),
    remark = '代码仓库项目接入与审查配置'
WHERE menu_id = 122;

-- ----------------------------
-- 5. 策略配置子菜单（模板迁入；模型/引擎排序）
-- ----------------------------
UPDATE sys_menu
SET parent_id = 4,
    order_num = 1,
    menu_name = '审查模板',
    update_by = 'admin',
    update_time = SYSDATE(),
    remark = '项目审查模板管理'
WHERE menu_id = 127;

UPDATE sys_menu
SET parent_id = 4,
    order_num = 2,
    menu_name = '大模型配置',
    update_by = 'admin',
    update_time = SYSDATE(),
    remark = '策略配置 - 大模型配置'
WHERE menu_id = 120;

UPDATE sys_menu
SET parent_id = 4,
    order_num = 3,
    menu_name = '审查引擎',
    update_by = 'admin',
    update_time = SYSDATE(),
    remark = '策略配置 - 本地审查引擎'
WHERE menu_id = 124;

-- ----------------------------
-- 6. 角色授权：拥有项目接入子菜单的角色补授父目录
-- ----------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, 6
FROM sys_role_menu rm
WHERE rm.menu_id IN (121, 122, 123)
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = 6
  );

INSERT INTO sys_role_menu SELECT '2', '6'
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '6');
