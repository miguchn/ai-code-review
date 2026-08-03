-- ----------------------------
-- M4：GitHub PR 审查结果回写（总结评论投递记录）
-- 日期: 2026-08-03
-- 前置: 08_github_pr_webhook.sql、13_review_pipeline_m3.sql、19_review_record_experience_m3_1.sql
-- 说明:
--   1) 新建 review_delivery_record：PR 级幂等投递事实（与审查任务状态分离）
--   2) 字典 review_delivery_status：SUCCESS / FAILED
--   3) 审查记录菜单下新增「投递重试」按钮权限 review:delivery:retry（menu_id=1150）
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/23_review_delivery_record.sql
-- ----------------------------

SET NAMES utf8mb4;

-- ----------------------------
-- 投递记录表：一次评论回写副作用（PR 级幂等）
-- ----------------------------
CREATE TABLE IF NOT EXISTS review_delivery_record (
  delivery_id        bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '投递ID',
  task_id            bigint(20)   NOT NULL COMMENT '最近一次关联的审查任务ID',
  run_id             bigint(20)   DEFAULT NULL COMMENT '最近一次关联的执行记录ID',
  project_id         bigint(20)   NOT NULL COMMENT '项目ID',
  provider           varchar(20)  NOT NULL DEFAULT 'GITHUB' COMMENT 'Git Provider',
  channel            varchar(40)  NOT NULL COMMENT '投递渠道(GITHUB_PR_SUMMARY_COMMENT)',
  pr_number          int(11)      NOT NULL COMMENT 'PR编号',
  idempotency_key    varchar(128) NOT NULL COMMENT '幂等键',
  external_id        varchar(64)  DEFAULT NULL COMMENT '外部评论ID（GitHub comment id）',
  delivery_status    varchar(20)  NOT NULL COMMENT '投递状态(SUCCESS/FAILED)',
  failure_message    varchar(500) DEFAULT NULL COMMENT '失败原因（已脱敏）',
  attempt_count      int(11)      NOT NULL DEFAULT 1 COMMENT '投递尝试次数',
  last_attempt_time  datetime     NOT NULL COMMENT '最近尝试时间',
  create_by          varchar(64)  DEFAULT '',
  create_time        datetime     DEFAULT NULL,
  update_by          varchar(64)  DEFAULT '',
  update_time        datetime     DEFAULT NULL,
  PRIMARY KEY (delivery_id),
  UNIQUE KEY uk_delivery_idempotency (idempotency_key),
  KEY idx_delivery_task (task_id),
  KEY idx_delivery_project_pr (project_id, pr_number),
  KEY idx_delivery_status (delivery_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审查结果投递记录';

-- ----------------------------
-- 投递状态字典
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审查投递状态', 'review_delivery_status', '0', 'admin', SYSDATE(), 'GitHub 评论等外部投递状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_delivery_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '已投递', 'SUCCESS', 'review_delivery_status', '', 'success', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_status' AND dict_value = 'SUCCESS');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '投递失败', 'FAILED', 'review_delivery_status', '', 'danger', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_status' AND dict_value = 'FAILED');

-- ----------------------------
-- 投递重试权限（挂在审查记录菜单下）
-- ----------------------------
INSERT INTO sys_menu
SELECT 1150, '投递重试', 128, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:delivery:retry', '#',
       'admin', SYSDATE(), '', NULL, '重试 GitHub PR 总结评论投递'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1150);

INSERT INTO sys_role_menu SELECT '2', '1150'
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1150');
