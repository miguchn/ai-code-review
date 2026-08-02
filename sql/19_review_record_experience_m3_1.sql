-- ----------------------------
-- M3.1 审查任务与审查记录体验优化
-- 日期: 2026-08-02
-- 前置: 16_review_scoring_result_protocol.sql、18_review_execution_hardening.sql
-- 说明:
--   1) review_task 补充提交者、增删行数（列表/记录展示用，历史可空）
--   2) 新增「审查记录」菜单与查询权限（复用 review_task / review_task_run，不新建业务表）
--   3) 调整代码审查菜单顺序：审查任务为执行队列，审查记录为已完成结果
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/19_review_record_experience_m3_1.sql
-- 注意: 必须使用 utf8mb4 连接；若已在错误字符集下执行导致乱码，再执行 20_review_record_charset_fix.sql
-- ----------------------------

SET NAMES utf8mb4;

-- ----------------------------
-- 1. review_task：PR 提交者与变更行数
-- ----------------------------
DROP PROCEDURE IF EXISTS upgrade_review_task_record_meta_m31;
DELIMITER $$
CREATE PROCEDURE upgrade_review_task_record_meta_m31()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'pr_author'
  ) THEN
    ALTER TABLE review_task
      ADD COLUMN pr_author varchar(100) DEFAULT NULL COMMENT 'PR 提交者（GitHub login）' AFTER pr_title;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'additions'
  ) THEN
    ALTER TABLE review_task
      ADD COLUMN additions int(11) DEFAULT NULL COMMENT 'PR 新增行数' AFTER head_sha;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'deletions'
  ) THEN
    ALTER TABLE review_task
      ADD COLUMN deletions int(11) DEFAULT NULL COMMENT 'PR 删除行数' AFTER additions;
  END IF;
END$$
DELIMITER ;
CALL upgrade_review_task_record_meta_m31();
DROP PROCEDURE IF EXISTS upgrade_review_task_record_meta_m31;

-- ----------------------------
-- 2. 菜单：审查记录 + 查询权限；调整顺序
-- ----------------------------
UPDATE sys_menu SET order_num = 5, update_by = 'admin', update_time = SYSDATE(),
    remark = '审查执行队列：待执行/执行中/失败与重试'
WHERE menu_id = 125;

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 128, '审查记录', 3, 6, 'record', 'review/record/index', '', 'ReviewRecord',
       1, 0, 'C', '0', '0', 'review:record:list', 'documentation',
       'admin', SYSDATE(), '', NULL, '已完成审查结果历史'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 128);

INSERT INTO sys_menu
SELECT 1149, '记录查询', 128, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:record:query', '#',
       'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1149);

INSERT INTO sys_role_menu SELECT '2', '128'
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '128');
INSERT INTO sys_role_menu SELECT '2', '1149'
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1149');
