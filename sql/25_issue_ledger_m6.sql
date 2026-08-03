-- ----------------------------
-- M6：问题台账基础闭环
-- 日期: 2026-08-03
-- 前置: 16_review_scoring_result_protocol.sql、23_review_delivery_record.sql、24_notification_management_m5.sql
-- 说明:
--   1) 新建 review_issue：PR 级指纹去重的可处置问题
--   2) 新建 review_issue_action：处置动作流水
--   3) 字典 review_issue_status / review_issue_close_source / review_issue_origin
--   4) 审查中心「问题台账」(131) 及按钮 1160-1163
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/25_issue_ledger_m6.sql
-- ----------------------------

SET NAMES utf8mb4;

-- ----------------------------
-- 1. review_issue
-- ----------------------------
CREATE TABLE IF NOT EXISTS review_issue (
  issue_id         bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '问题ID',
  project_id       bigint(20)    NOT NULL COMMENT '项目ID',
  provider         varchar(20)   NOT NULL DEFAULT 'GITHUB' COMMENT 'Git Provider',
  pr_number        int(11)       NOT NULL COMMENT 'PR编号',
  fingerprint      varchar(80)   NOT NULL COMMENT '问题指纹(PR级去重)',
  first_task_id    bigint(20)    NOT NULL COMMENT '首次发现任务ID',
  first_run_id     bigint(20)    DEFAULT NULL COMMENT '首次发现执行记录ID',
  last_task_id     bigint(20)    NOT NULL COMMENT '最近物化任务ID',
  last_run_id      bigint(20)    DEFAULT NULL COMMENT '最近物化执行记录ID',
  issue_rank       int(11)       DEFAULT NULL COMMENT '最近一次Top3位次',
  severity         varchar(20)   DEFAULT NULL COMMENT '严重度',
  category         varchar(100)  DEFAULT NULL COMMENT '类别',
  title            varchar(500)  NOT NULL COMMENT '标题',
  description      mediumtext    DEFAULT NULL COMMENT '描述',
  file_path        varchar(500)  DEFAULT NULL COMMENT '文件路径',
  start_line       int(11)       DEFAULT NULL COMMENT '起始行',
  end_line         int(11)       DEFAULT NULL COMMENT '结束行',
  evidence         mediumtext    DEFAULT NULL COMMENT '证据',
  suggestion       mediumtext    DEFAULT NULL COMMENT '建议',
  origin           varchar(20)   NOT NULL DEFAULT 'NEW' COMMENT '归属(NEW/EXISTING)',
  status           varchar(30)   NOT NULL COMMENT '状态',
  resolve_note     varchar(500)  DEFAULT NULL COMMENT '处理说明(关闭选填/忽略误报必填)',
  close_source     varchar(20)   DEFAULT NULL COMMENT '关闭来源(manual/auto_recheck)',
  closed_by        varchar(64)   DEFAULT NULL COMMENT '终态操作者',
  closed_time      datetime      DEFAULT NULL COMMENT '终态时间',
  create_by        varchar(64)   DEFAULT '',
  create_time      datetime      DEFAULT NULL,
  update_by        varchar(64)   DEFAULT '',
  update_time      datetime      DEFAULT NULL,
  PRIMARY KEY (issue_id),
  UNIQUE KEY uk_issue_pr_fingerprint (project_id, pr_number, fingerprint),
  KEY idx_issue_project_status (project_id, status),
  KEY idx_issue_origin_status (origin, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审查问题台账';

-- ----------------------------
-- 2. review_issue_action
-- ----------------------------
CREATE TABLE IF NOT EXISTS review_issue_action (
  action_id      bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '动作ID',
  issue_id       bigint(20)    NOT NULL COMMENT '问题ID',
  operator       varchar(64)   NOT NULL COMMENT '操作者',
  action_type    varchar(30)   NOT NULL COMMENT '动作类型(CONFIRM/CLOSE/DISMISS)',
  from_status    varchar(30)   NOT NULL COMMENT '原状态',
  to_status      varchar(30)   NOT NULL COMMENT '新状态',
  resolve_note   varchar(500)  DEFAULT NULL COMMENT '处理说明快照',
  create_time    datetime      NOT NULL COMMENT '动作时间',
  PRIMARY KEY (action_id),
  KEY idx_issue_action_issue_time (issue_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审查问题动作流水';

-- ----------------------------
-- 3. 字典
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审查问题状态', 'review_issue_status', '0', 'admin', SYSDATE(), '问题台账状态（含 RECHECKING 预留）'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_issue_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '待确认', 'AWAITING_CONFIRM', 'review_issue_status', '', 'info', 'Y', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_issue_status' AND dict_value = 'AWAITING_CONFIRM');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '待修复', 'AWAITING_FIX', 'review_issue_status', '', 'warning', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_issue_status' AND dict_value = 'AWAITING_FIX');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已关闭', 'CLOSED', 'review_issue_status', '', 'success', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_issue_status' AND dict_value = 'CLOSED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已忽略', 'IGNORED', 'review_issue_status', '', 'info', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_issue_status' AND dict_value = 'IGNORED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '误报', 'FALSE_POSITIVE', 'review_issue_status', '', 'danger', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_issue_status' AND dict_value = 'FALSE_POSITIVE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '复核中', 'RECHECKING', 'review_issue_status', '', 'primary', 'N', '0', 'admin', SYSDATE(), '预留，本期不实现'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_issue_status' AND dict_value = 'RECHECKING');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审查问题关闭来源', 'review_issue_close_source', '0', 'admin', SYSDATE(), 'manual / auto_recheck（预留）'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_issue_close_source');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '人工', 'manual', 'review_issue_close_source', '', 'primary', 'Y', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_issue_close_source' AND dict_value = 'manual');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '自动复核', 'auto_recheck', 'review_issue_close_source', '', 'info', 'N', '0', 'admin', SYSDATE(), '预留'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_issue_close_source' AND dict_value = 'auto_recheck');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审查问题归属', 'review_issue_origin', '0', 'admin', SYSDATE(), 'NEW / EXISTING'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_issue_origin');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '新增', 'NEW', 'review_issue_origin', '', 'success', 'Y', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_issue_origin' AND dict_value = 'NEW');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '存量', 'EXISTING', 'review_issue_origin', '', 'info', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_issue_origin' AND dict_value = 'EXISTING');

-- ----------------------------
-- 4. 菜单与权限（menu_id 核验：131 / 1160-1163 空闲）
-- ----------------------------
INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 131, '问题台账', 3, 7, 'issue', 'review/issue/index', '', 'ReviewIssue',
       1, 0, 'C', '0', '0', 'review:issue:list', 'bug',
       'admin', SYSDATE(), '', NULL, '审查问题确认与关闭'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 131);

INSERT INTO sys_menu SELECT 1160, '问题列表', 131, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:issue:list',    '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1160);
INSERT INTO sys_menu SELECT 1161, '问题查询', 131, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:issue:query',   '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1161);
INSERT INTO sys_menu SELECT 1162, '问题确认', 131, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:issue:confirm', '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1162);
INSERT INTO sys_menu SELECT 1163, '问题关闭', 131, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:issue:close',   '#', 'admin', SYSDATE(), '', NULL, '覆盖关闭/忽略/误报'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1163);

INSERT INTO sys_role_menu SELECT '2', '131' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '131');
INSERT INTO sys_role_menu SELECT '2', '1160' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1160');
INSERT INTO sys_role_menu SELECT '2', '1161' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1161');
INSERT INTO sys_role_menu SELECT '2', '1162' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1162');
INSERT INTO sys_role_menu SELECT '2', '1163' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1163');
