-- ----------------------------
-- P0/M2 GitHub PR Webhook 事件接入增量脚本
-- 日期: 2026-08-01
-- 前置: 01_core_schema.sql、02_quartz_schema.sql、03_system_management.sql、
--       04_github_project_access.sql、05_github_pr_scope.sql
-- 说明: 新增 Webhook 事件表、审查任务表、项目表 Webhook 列，
--       以及“审查任务”菜单与功能权限。
-- ----------------------------

-- ----------------------------
-- Webhook 事件表：一次可信投递的不可变接入事实
-- ----------------------------
CREATE TABLE IF NOT EXISTS review_webhook_event (
  event_id           bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '事件ID',
  provider           varchar(20)  NOT NULL DEFAULT 'GITHUB' COMMENT 'Git Provider',
  delivery_id        varchar(64)  NOT NULL COMMENT '平台投递ID（幂等键）',
  event_type         varchar(40)  NOT NULL COMMENT '事件类型(pull_request/ping等)',
  action             varchar(40)  DEFAULT NULL COMMENT 'PR动作(opened/reopened/synchronize等)',
  repository_owner   varchar(100) DEFAULT NULL COMMENT '仓库所有者',
  repository_name    varchar(100) DEFAULT NULL COMMENT '仓库名称',
  project_id         bigint(20)   DEFAULT NULL COMMENT '匹配的项目ID',
  pr_number          int(11)      DEFAULT NULL COMMENT 'PR编号',
  pr_title           varchar(500) DEFAULT NULL COMMENT 'PR标题',
  source_branch      varchar(255) DEFAULT NULL COMMENT '来源分支',
  target_branch      varchar(255) DEFAULT NULL COMMENT '目标分支',
  base_sha           varchar(64)  DEFAULT NULL COMMENT 'base SHA',
  head_sha           varchar(64)  DEFAULT NULL COMMENT 'head SHA',
  process_status     varchar(20)  NOT NULL DEFAULT 'RECEIVED' COMMENT '处理状态(RECEIVED/ACCEPTED/IGNORED/DUPLICATE/FAILED)',
  process_message    varchar(500) DEFAULT NULL COMMENT '处理结果说明',
  duplicate_count    int(11)      NOT NULL DEFAULT 0 COMMENT '重复投递次数',
  task_id            bigint(20)   DEFAULT NULL COMMENT '生成的审查任务ID',
  payload_size       int(11)      NOT NULL DEFAULT 0 COMMENT '载荷字节数',
  receive_time       datetime     NOT NULL COMMENT '接收时间',
  process_time       datetime    DEFAULT NULL COMMENT '处理完成时间',
  create_by          varchar(64)  DEFAULT '',
  create_time        datetime     DEFAULT NULL,
  PRIMARY KEY (event_id),
  UNIQUE KEY uk_webhook_delivery (provider, delivery_id),
  KEY idx_webhook_project (project_id),
  KEY idx_webhook_status (process_status),
  KEY idx_webhook_receive_time (receive_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Webhook事件表';

-- ----------------------------
-- 审查任务表：一次由事件触发的执行实例
-- ----------------------------
CREATE TABLE IF NOT EXISTS review_task (
  task_id         bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  project_id      bigint(20)   NOT NULL COMMENT '项目ID',
  event_id        bigint(20)   NOT NULL COMMENT '触发事件ID',
  provider        varchar(20)  NOT NULL DEFAULT 'GITHUB' COMMENT 'Git Provider',
  pr_number       int(11)      NOT NULL COMMENT 'PR编号',
  pr_title        varchar(500) DEFAULT NULL COMMENT 'PR标题',
  source_branch   varchar(255) NOT NULL COMMENT '来源分支',
  target_branch   varchar(255) NOT NULL COMMENT '目标分支',
  base_sha        varchar(64)  NOT NULL COMMENT 'base SHA（事件版本）',
  head_sha        varchar(64)  NOT NULL COMMENT 'head SHA（事件版本）',
  trigger_type    varchar(20)  NOT NULL DEFAULT 'WEBHOOK' COMMENT '触发方式(WEBHOOK/MANUAL/SCHEDULE)',
  task_status     varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT '任务状态(PENDING/RUNNING/SUCCESS/FAILED/CANCELLED)',
  failure_message varchar(500) DEFAULT NULL COMMENT '执行失败原因',
  remark          varchar(500) DEFAULT '',
  create_by       varchar(64)  DEFAULT '',
  create_time     datetime     DEFAULT NULL,
  update_by       varchar(64)  DEFAULT '',
  update_time     datetime     DEFAULT NULL,
  PRIMARY KEY (task_id),
  UNIQUE KEY uk_task_event (event_id),
  KEY idx_task_project (project_id),
  KEY idx_task_status (task_status),
  KEY idx_task_pr (project_id, pr_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审查任务表';

-- ----------------------------
-- 项目表 Webhook 增量列（幂等）
-- ----------------------------
DROP PROCEDURE IF EXISTS upgrade_review_project_webhook;
DELIMITER $$
CREATE PROCEDURE upgrade_review_project_webhook()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'webhook_secret_ciphertext'
  ) THEN
    ALTER TABLE review_project
      ADD COLUMN webhook_secret_ciphertext varchar(1000) DEFAULT NULL COMMENT 'Webhook Secret 密文（AES-GCM）' AFTER last_branch_sync_time;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'last_webhook_time'
  ) THEN
    ALTER TABLE review_project
      ADD COLUMN last_webhook_time datetime DEFAULT NULL COMMENT '最近 Webhook 接收时间' AFTER webhook_secret_ciphertext;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'last_webhook_result'
  ) THEN
    ALTER TABLE review_project
      ADD COLUMN last_webhook_result varchar(500) DEFAULT NULL COMMENT '最近 Webhook 接收结果' AFTER last_webhook_time;
  END IF;
END$$
DELIMITER ;
CALL upgrade_review_project_webhook();
DROP PROCEDURE IF EXISTS upgrade_review_project_webhook;

-- ----------------------------
-- “审查任务”菜单与功能权限
-- ----------------------------
INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 125, '审查任务', 3, 4, 'task', 'review/task/index', '', 'ReviewTask',
       1, 0, 'C', '0', '0', 'review:task:list', 'list',
       'admin', SYSDATE(), '', NULL, 'PR 审查任务列表'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 125);

UPDATE sys_menu SET parent_id = 3, order_num = 4, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 125;

INSERT INTO sys_menu SELECT 1135, '任务查询', 125, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:task:query', '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1135);

-- ----------------------------
-- 业务字典：任务状态、事件处理状态、触发方式
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审查任务状态', 'review_task_status', '0', 'admin', SYSDATE(), '审查任务执行状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_task_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '待执行', 'PENDING', 'review_task_status', '', 'info', 'Y', '0', 'admin', SYSDATE(), '' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_status' AND dict_value = 'PENDING');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '执行中', 'RUNNING', 'review_task_status', '', 'primary', 'N', '0', 'admin', SYSDATE(), '' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_status' AND dict_value = 'RUNNING');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已完成', 'SUCCESS', 'review_task_status', '', 'success', 'N', '0', 'admin', SYSDATE(), '' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_status' AND dict_value = 'SUCCESS');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已失败', 'FAILED', 'review_task_status', '', 'danger', 'N', '0', 'admin', SYSDATE(), '' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_status' AND dict_value = 'FAILED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '已取消', 'CANCELLED', 'review_task_status', '', 'warning', 'N', '0', 'admin', SYSDATE(), '' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_status' AND dict_value = 'CANCELLED');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT 'Webhook事件处理状态', 'review_webhook_process_status', '0', 'admin', SYSDATE(), 'Webhook 事件接入处理状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_webhook_process_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '已接收', 'RECEIVED', 'review_webhook_process_status', '', 'info', 'Y', '0', 'admin', SYSDATE(), '' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_webhook_process_status' AND dict_value = 'RECEIVED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '已受理', 'ACCEPTED', 'review_webhook_process_status', '', 'success', 'N', '0', 'admin', SYSDATE(), '' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_webhook_process_status' AND dict_value = 'ACCEPTED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已忽略', 'IGNORED', 'review_webhook_process_status', '', 'info', 'N', '0', 'admin', SYSDATE(), '' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_webhook_process_status' AND dict_value = 'IGNORED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '重复投递', 'DUPLICATE', 'review_webhook_process_status', '', 'warning', 'N', '0', 'admin', SYSDATE(), '' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_webhook_process_status' AND dict_value = 'DUPLICATE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '接入失败', 'FAILED', 'review_webhook_process_status', '', 'danger', 'N', '0', 'admin', SYSDATE(), '' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_webhook_process_status' AND dict_value = 'FAILED');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审查任务触发方式', 'review_trigger_type', '0', 'admin', SYSDATE(), '审查任务触发方式'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_trigger_type');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, 'Webhook事件', 'WEBHOOK', 'review_trigger_type', '', 'primary', 'Y', '0', 'admin', SYSDATE(), '' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_trigger_type' AND dict_value = 'WEBHOOK');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '人工触发', 'MANUAL', 'review_trigger_type', '', 'success', 'N', '0', 'admin', SYSDATE(), '' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_trigger_type' AND dict_value = 'MANUAL');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '定时调度', 'SCHEDULE', 'review_trigger_type', '', 'warning', 'N', '0', 'admin', SYSDATE(), '' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_trigger_type' AND dict_value = 'SCHEDULE');
