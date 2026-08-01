-- ----------------------------
-- 修复大模型配置菜单中文乱码
-- 日期: 2026-08-01
-- 前置: 09_llm_custom_provider.sql
-- 说明: 09 在未指定 utf8mb4 连接字符集时执行，导致菜单文案双重编码；
--       本脚本按 menu_id 重写为目标中文。
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/10_llm_menu_charset_fix.sql
-- ----------------------------

SET NAMES utf8mb4;

UPDATE sys_menu
SET menu_name = '大模型配置',
    remark = '模型服务 - 大模型配置',
    update_by = 'admin',
    update_time = SYSDATE()
WHERE menu_id = 120;

UPDATE sys_menu SET menu_name = '模型查询', update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 1116;
UPDATE sys_menu SET menu_name = '模型新增', update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 1117;
UPDATE sys_menu SET menu_name = '模型修改', update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 1118;
UPDATE sys_menu SET menu_name = '模型删除', update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 1119;
