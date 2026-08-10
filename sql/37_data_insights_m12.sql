-- ----------------------------
-- 37_data_insights_m12.sql
-- M12 数据洞察一期：review_stats_daily 聚合表、一级菜单与权限
--
-- 部署边界：幂等可重跑。不注册 Quartz cron（由运维在「定时任务」中配置
-- insightStatsJobTask.fullRecalc() / insightStatsJobTask.refreshRecent()）。
-- 须 utf8mb4 连接执行。
-- ----------------------------

DROP PROCEDURE IF EXISTS upgrade_data_insights_m12;
DELIMITER $$
CREATE PROCEDURE upgrade_data_insights_m12()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'review_stats_daily') THEN
    CREATE TABLE review_stats_daily (
      id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
      project_id bigint NOT NULL COMMENT '项目ID',
      stat_date date NOT NULL COMMENT '统计日',
      task_total int NOT NULL DEFAULT 0 COMMENT '终态任务数(SUCCESS+FAILED)',
      task_success int NOT NULL DEFAULT 0 COMMENT '成功任务数',
      task_failed int NOT NULL DEFAULT 0 COMMENT '失败任务数',
      task_push int NOT NULL DEFAULT 0 COMMENT 'PUSH 来源任务数',
      task_covered int NOT NULL DEFAULT 0 COMMENT 'SUCCESS且至少一次投递SUCCESS的任务数',
      duration_p95_ms bigint NOT NULL DEFAULT 0 COMMENT '成功任务 duration_ms 的 P95',
      issue_new int NOT NULL DEFAULT 0 COMMENT '新增问题(origin=NEW)',
      issue_critical int NOT NULL DEFAULT 0 COMMENT '新增 CRITICAL',
      issue_high int NOT NULL DEFAULT 0 COMMENT '新增 HIGH',
      issue_medium int NOT NULL DEFAULT 0 COMMENT '新增 MEDIUM',
      issue_low int NOT NULL DEFAULT 0 COMMENT '新增 LOW',
      issue_closed int NOT NULL DEFAULT 0 COMMENT '当日关闭(CLOSED)',
      issue_confirmed int NOT NULL DEFAULT 0 COMMENT '当日确认(CONFIRM动作)',
      issue_false_positive int NOT NULL DEFAULT 0 COMMENT '当日误报(FALSE_POSITIVE)',
      delivery_total int NOT NULL DEFAULT 0 COMMENT '投递尝试数(终态)',
      delivery_success int NOT NULL DEFAULT 0 COMMENT '投递成功数',
      event_accepted int NOT NULL DEFAULT 0 COMMENT 'ACCEPTED 事件数',
      event_ignored int NOT NULL DEFAULT 0 COMMENT 'IGNORED 事件数',
      create_time datetime DEFAULT NULL COMMENT '创建时间',
      update_time datetime DEFAULT NULL COMMENT '更新时间',
      PRIMARY KEY (id),
      UNIQUE KEY uk_stats_project_date (project_id, stat_date),
      KEY idx_stats_date (stat_date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审查日聚合(项目×日)';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_stats_daily' AND column_name = 'task_covered') THEN
    ALTER TABLE review_stats_daily ADD COLUMN task_covered int NOT NULL DEFAULT 0 COMMENT 'SUCCESS且至少一次投递SUCCESS的任务数' AFTER task_push;
  END IF;
END$$
DELIMITER ;
CALL upgrade_data_insights_m12();
DROP PROCEDURE IF EXISTS upgrade_data_insights_m12;

-- 指标口径版本（看板页脚 / 指标说明展示）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '数据洞察-指标口径版本', 'insight.metrics.dict.version', 'm12-v1', 'Y', 'admin', SYSDATE(), '', NULL, '看板指标字典口径版本号'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'insight.metrics.dict.version');

-- ----------------------------
-- 菜单：数据洞察（位于审查中心之后、项目接入之前）
-- menu_id：7 / 132-134 / 1170-1171
-- ----------------------------
INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 7, '数据洞察', 0, 2, 'insight', NULL, '', '',
       1, 0, 'M', '0', '0', '', 'chart',
       'admin', SYSDATE(), '', NULL, '数据洞察：总览看板、项目分析、报告中心'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 7);

UPDATE sys_menu SET order_num = 1, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 3;
UPDATE sys_menu SET order_num = 2, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 7;
UPDATE sys_menu SET order_num = 3, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 6;
UPDATE sys_menu SET order_num = 4, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 4;
UPDATE sys_menu SET order_num = 5, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 5;
UPDATE sys_menu SET order_num = 6, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 1;
UPDATE sys_menu SET order_num = 7, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 2;

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 132, '总览看板', 7, 1, 'overview', 'insight/overview/index', '', 'InsightOverview',
       1, 0, 'C', '0', '0', 'insight:overview:view', 'dashboard',
       'admin', SYSDATE(), '', NULL, '平台治理健康度总览'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 132);

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 133, '项目分析', 7, 2, 'project', 'insight/project/index', '', 'InsightProject',
       1, 0, 'C', '0', '0', 'insight:project:view', 'list',
       'admin', SYSDATE(), '', NULL, '项目指标矩阵与趋势下钻'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 133);

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 134, '报告中心', 7, 3, 'report', 'insight/report/placeholder', '', 'InsightReport',
       1, 0, 'C', '0', '0', 'insight:overview:view', 'documentation',
       'admin', SYSDATE(), '', NULL, '三期占位：周/月快照与导出'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 134);

INSERT INTO sys_menu SELECT 1170, '总览查看', 132, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'insight:overview:view', '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1170);
INSERT INTO sys_menu SELECT 1171, '项目查看', 133, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'insight:project:view', '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1171);

-- 普通角色（role_id=2）授权；超级管理员(role_id=1)默认全量权限
INSERT INTO sys_role_menu SELECT '2', '7' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '7');
INSERT INTO sys_role_menu SELECT '2', '132' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '132');
INSERT INTO sys_role_menu SELECT '2', '133' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '133');
INSERT INTO sys_role_menu SELECT '2', '134' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '134');
INSERT INTO sys_role_menu SELECT '2', '1170' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1170');
INSERT INTO sys_role_menu SELECT '2', '1171' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1171');
