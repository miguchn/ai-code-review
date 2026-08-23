-- ----------------------------
-- 49_issue_auto_assignment.sql
-- 问题自动指派闭环（M13）：责任人列、逾期标志、逾期参数/字典、逾期扫描任务
--
-- 前置: 48_token_usage_analysis.sql、42_identity_binding.sql、41_sys_job_seed.sql、26_issue_delivery_trace_m6_1.sql
-- 部署边界：幂等可重跑。须 utf8mb4 连接执行。不建新表、不回填历史责任人。
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/49_issue_auto_assignment.sql
-- ----------------------------

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS upgrade_issue_auto_assignment;
DELIMITER $$
CREATE PROCEDURE upgrade_issue_auto_assignment()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'review_issue'
      AND column_name = 'assignee_user_id'
  ) THEN
    ALTER TABLE review_issue
      ADD COLUMN assignee_user_id bigint DEFAULT NULL COMMENT '责任人平台用户ID(自动推断或转派)';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'review_issue'
      AND column_name = 'assign_source'
  ) THEN
    ALTER TABLE review_issue
      ADD COLUMN assign_source varchar(20) DEFAULT NULL COMMENT '指派来源(AUTO_COMMIT/AUTO_PR_AUTHOR/AUTO_OWNER/TRANSFER)';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'review_issue'
      AND column_name = 'assign_time'
  ) THEN
    ALTER TABLE review_issue
      ADD COLUMN assign_time datetime DEFAULT NULL COMMENT '最近指派时间';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'review_issue'
      AND column_name = 'overdue_flag'
  ) THEN
    ALTER TABLE review_issue
      ADD COLUMN overdue_flag char(1) NOT NULL DEFAULT 'N' COMMENT '逾期标志(逾期任务每日重算)';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'review_issue'
      AND index_name = 'idx_issue_assignee'
  ) THEN
    ALTER TABLE review_issue
      ADD KEY idx_issue_assignee (project_id, assignee_user_id, status);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'review_issue'
      AND index_name = 'idx_issue_overdue'
  ) THEN
    ALTER TABLE review_issue
      ADD KEY idx_issue_overdue (overdue_flag, status);
  END IF;
END$$
DELIMITER ;

CALL upgrade_issue_auto_assignment();
DROP PROCEDURE IF EXISTS upgrade_issue_auto_assignment;

-- 逾期天数参数（运行期可调）
INSERT INTO sys_config
  (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '问题台账-高严重度逾期天数', 'review.issue.overdue.highDays', '3', 'Y', 'admin', SYSDATE(), '', NULL,
       'CRITICAL/HIGH 严重度：进入当前状态超过该天数则标逾期'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.issue.overdue.highDays');

INSERT INTO sys_config
  (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '问题台账-普通严重度逾期天数', 'review.issue.overdue.normalDays', '7', 'Y', 'admin', SYSDATE(), '', NULL,
       '非 CRITICAL/HIGH 严重度：进入当前状态超过该天数则标逾期'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.issue.overdue.normalDays');

-- 投递触发来源：逾期提醒（每项目每日一条，不走项目冷却）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '逾期提醒', 'OVERDUE_REMIND', 'review_delivery_trigger_source', '', 'danger', 'N', '0', 'admin', SYSDATE(),
       '逾期聚合提醒按每项目每日一条自治频控，不走项目冷却'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_trigger_source' AND dict_value = 'OVERDUE_REMIND');

-- 逾期扫描任务（仿 sql/41 幂等注册）
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '问题台账-逾期扫描提醒', 'DEFAULT', 'issueOverdueJobTask.scan', '0 30 8 * * ?', '3', '1', '0', 'admin', SYSDATE(),
       '每日 08:30 重算逾期标志；新转逾期且项目启用通知时入队聚合提醒'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'issueOverdueJobTask.scan');
