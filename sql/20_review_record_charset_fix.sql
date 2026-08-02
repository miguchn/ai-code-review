-- ----------------------------
-- 修复 M3.1 审查记录相关中文乱码
-- 日期: 2026-08-02
-- 前置: 19_review_record_experience_m3_1.sql
-- 说明: 19 在未指定 utf8mb4 连接字符集时执行，导致菜单文案与列注释双重编码；
--       本脚本按 menu_id / 列名重写为目标中文。
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/20_review_record_charset_fix.sql
-- ----------------------------

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 菜单文案
-- ----------------------------
UPDATE sys_menu
SET menu_name = '审查任务',
    remark = '审查执行队列：待执行/执行中/失败与重试',
    update_by = 'admin',
    update_time = SYSDATE()
WHERE menu_id = 125;

UPDATE sys_menu
SET menu_name = '审查记录',
    remark = '已完成审查结果历史',
    update_by = 'admin',
    update_time = SYSDATE()
WHERE menu_id = 128;

UPDATE sys_menu
SET menu_name = '记录查询',
    update_by = 'admin',
    update_time = SYSDATE()
WHERE menu_id = 1149;

-- ----------------------------
-- 2. review_task 列注释
-- ----------------------------
ALTER TABLE review_task
  MODIFY COLUMN pr_author varchar(100) DEFAULT NULL COMMENT 'PR 发起人（GitHub login）',
  MODIFY COLUMN additions int(11) DEFAULT NULL COMMENT 'PR 新增行数',
  MODIFY COLUMN deletions int(11) DEFAULT NULL COMMENT 'PR 删除行数';
