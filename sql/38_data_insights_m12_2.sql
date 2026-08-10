-- ----------------------------
-- 38_data_insights_m12_2.sql
-- M12 数据洞察二期：提交事实、成员日聚合、身份认领、成员分析菜单
--
-- 部署边界：须先执行 37_data_insights_m12.sql。本脚本幂等可重跑。须 utf8mb4。
-- ----------------------------

DROP PROCEDURE IF EXISTS upgrade_data_insights_m12_2;
DELIMITER $$
CREATE PROCEDURE upgrade_data_insights_m12_2()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'review_commit_fact') THEN
    CREATE TABLE review_commit_fact (
      id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
      project_id bigint NOT NULL COMMENT '项目ID',
      commit_sha varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '提交 SHA',
      commit_time datetime NOT NULL COMMENT '提交时间',
      author_name varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作者名',
      author_email varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作者邮箱',
      message_first_line varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '提交消息首行',
      source_event_id bigint DEFAULT NULL COMMENT '抽取来源 Webhook 事件ID',
      create_time datetime DEFAULT NULL COMMENT '入库时间',
      PRIMARY KEY (id),
      UNIQUE KEY uk_commit_project_sha (project_id, commit_sha),
      KEY idx_commit_author_time (project_id, author_email, commit_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='推送提交事实';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'review_member_stats_daily') THEN
    CREATE TABLE review_member_stats_daily (
      id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
      project_id bigint NOT NULL COMMENT '项目ID',
      author_key varchar(320) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '提交身份键(邮箱优先否则名称)',
      author_name varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '展示名',
      stat_date date NOT NULL COMMENT '统计日',
      commit_count int NOT NULL DEFAULT 0 COMMENT '提交数',
      tasks_reviewed int NOT NULL DEFAULT 0 COMMENT '被审任务数(pr_author弱匹配)',
      issues_new int NOT NULL DEFAULT 0 COMMENT '关联任务新增问题数',
      issues_open int NOT NULL DEFAULT 0 COMMENT '关联任务未关闭问题数(当日快照)',
      create_time datetime DEFAULT NULL COMMENT '创建时间',
      update_time datetime DEFAULT NULL COMMENT '更新时间',
      PRIMARY KEY (id),
      UNIQUE KEY uk_member_stats (project_id, author_key, stat_date),
      KEY idx_member_stats_date (stat_date),
      KEY idx_member_stats_author (author_key, stat_date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成员日聚合(项目×身份×日)';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'review_insight_identity_claim') THEN
    CREATE TABLE review_insight_identity_claim (
      id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
      user_id bigint NOT NULL COMMENT '平台用户ID',
      author_email varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '认领邮箱',
      author_name varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '认领展示名',
      create_by varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '',
      create_time datetime DEFAULT NULL,
      PRIMARY KEY (id),
      UNIQUE KEY uk_insight_claim_user_email (user_id, author_email),
      KEY idx_insight_claim_email (author_email)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据洞察提交身份认领';
  END IF;
END$$
DELIMITER ;
CALL upgrade_data_insights_m12_2();
DROP PROCEDURE IF EXISTS upgrade_data_insights_m12_2;

-- 成员分析菜单（报告中心顺序后移）
INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 135, '成员分析', 7, 3, 'member', 'insight/member/index', '', 'InsightMember',
       1, 0, 'C', '0', '0', 'insight:team:view', 'peoples',
       'admin', SYSDATE(), '', NULL, '本人自查 + 授权团队聚合（非绩效评价输入）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 135);

UPDATE sys_menu SET order_num = 4, update_by = 'admin', update_time = SYSDATE()
WHERE menu_id = 134;

INSERT INTO sys_menu SELECT 1172, '团队查看', 135, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'insight:team:view', '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1172);

INSERT INTO sys_role_menu SELECT '2', '135' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '135');
INSERT INTO sys_role_menu SELECT '2', '1172' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1172');
