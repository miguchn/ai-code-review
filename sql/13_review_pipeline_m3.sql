-- ----------------------------
-- P0/M3 真实代码审查全流程打通增量脚本
-- 日期: 2026-08-01
-- 前置: 08_github_pr_webhook.sql、06_llm_model_service.sql、07_review_engine.sql、12_review_engine_button_fix.sql
-- 说明: 项目审查配置、任务执行摘要、执行历史表、任务重试权限与业务字典。
-- ----------------------------

-- ----------------------------
-- 项目表：审查方式 / 引擎 / 模型绑定
-- ----------------------------
DROP PROCEDURE IF EXISTS upgrade_review_project_pipeline_m3;
DELIMITER $$
CREATE PROCEDURE upgrade_review_project_pipeline_m3()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'model_id'
  ) THEN
    ALTER TABLE review_project
      ADD COLUMN model_id bigint(20) DEFAULT NULL COMMENT '审查模型配置ID（空则回退平台默认模型）' AFTER credential_id;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'review_mode'
  ) THEN
    ALTER TABLE review_project
      ADD COLUMN review_mode varchar(40) NOT NULL DEFAULT 'OCR_PR_DIFF' COMMENT '审查方式(OCR_PR_DIFF=PR增量审查)' AFTER model_id;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'engine_code'
  ) THEN
    ALTER TABLE review_project
      ADD COLUMN engine_code varchar(40) NOT NULL DEFAULT 'OPEN_CODE_REVIEW' COMMENT '审查引擎编码' AFTER review_mode;
  END IF;
END$$
DELIMITER ;
CALL upgrade_review_project_pipeline_m3();
DROP PROCEDURE IF EXISTS upgrade_review_project_pipeline_m3;

-- ----------------------------
-- 任务表：当前执行摘要（历史细节在 review_task_run）
-- ----------------------------
DROP PROCEDURE IF EXISTS upgrade_review_task_pipeline_m3;
DELIMITER $$
CREATE PROCEDURE upgrade_review_task_pipeline_m3()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'review_conclusion'
  ) THEN
    ALTER TABLE review_task
      ADD COLUMN review_conclusion varchar(20) DEFAULT NULL COMMENT '审查结论(PASS/WARN/BLOCK)' AFTER task_status;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'current_step'
  ) THEN
    ALTER TABLE review_task
      ADD COLUMN current_step varchar(40) DEFAULT NULL COMMENT '当前/最近执行步骤' AFTER review_conclusion;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'failure_step'
  ) THEN
    ALTER TABLE review_task
      ADD COLUMN failure_step varchar(40) DEFAULT NULL COMMENT '失败步骤' AFTER current_step;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'failure_type'
  ) THEN
    ALTER TABLE review_task
      ADD COLUMN failure_type varchar(40) DEFAULT NULL COMMENT '失败分类' AFTER failure_step;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'attempt_count'
  ) THEN
    ALTER TABLE review_task
      ADD COLUMN attempt_count int(11) NOT NULL DEFAULT 0 COMMENT '执行尝试次数' AFTER failure_message;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'latest_run_id'
  ) THEN
    ALTER TABLE review_task
      ADD COLUMN latest_run_id bigint(20) DEFAULT NULL COMMENT '最近一次执行记录ID' AFTER attempt_count;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'started_time'
  ) THEN
    ALTER TABLE review_task
      ADD COLUMN started_time datetime DEFAULT NULL COMMENT '最近一次开始执行时间' AFTER latest_run_id;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'finished_time'
  ) THEN
    ALTER TABLE review_task
      ADD COLUMN finished_time datetime DEFAULT NULL COMMENT '最近一次结束执行时间' AFTER started_time;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'duration_ms'
  ) THEN
    ALTER TABLE review_task
      ADD COLUMN duration_ms bigint(20) DEFAULT NULL COMMENT '最近一次执行耗时(毫秒)' AFTER finished_time;
  END IF;
END$$
DELIMITER ;
CALL upgrade_review_task_pipeline_m3();
DROP PROCEDURE IF EXISTS upgrade_review_task_pipeline_m3;

-- ----------------------------
-- 执行历史表：每次领取/重试一条，禁止覆盖历史有效结果
-- ----------------------------
CREATE TABLE IF NOT EXISTS review_task_run (
  run_id                 bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '执行记录ID',
  task_id                bigint(20)    NOT NULL COMMENT '审查任务ID',
  attempt_no             int(11)       NOT NULL COMMENT '第几次尝试（从1递增）',
  run_status             varchar(20)   NOT NULL DEFAULT 'RUNNING' COMMENT '执行状态(RUNNING/SUCCESS/FAILED)',
  current_step           varchar(40)   DEFAULT NULL COMMENT '当前步骤',
  failure_step           varchar(40)   DEFAULT NULL COMMENT '失败步骤',
  failure_type           varchar(40)   DEFAULT NULL COMMENT '失败分类',
  failure_message        varchar(500)  DEFAULT NULL COMMENT '失败原因（中文）',
  review_conclusion      varchar(20)   DEFAULT NULL COMMENT '审查结论(PASS/WARN/BLOCK)',
  snapshot_review_mode   varchar(40)   NOT NULL COMMENT '快照：审查方式',
  snapshot_engine_code   varchar(40)   NOT NULL COMMENT '快照：引擎编码',
  snapshot_engine_name   varchar(100)  DEFAULT NULL COMMENT '快照：引擎名称',
  snapshot_engine_version varchar(64)  DEFAULT NULL COMMENT '快照：引擎版本',
  snapshot_model_id      bigint(20)    DEFAULT NULL COMMENT '快照：模型配置ID',
  snapshot_model_name    varchar(100)  DEFAULT NULL COMMENT '快照：模型配置名称',
  snapshot_model_provider varchar(40)  DEFAULT NULL COMMENT '快照：模型厂商',
  snapshot_model         varchar(100)  DEFAULT NULL COMMENT '快照：实际 Model 名',
  snapshot_timeout_seconds int(11)     DEFAULT NULL COMMENT '快照：超时秒数',
  snapshot_base_sha      varchar(64)   NOT NULL COMMENT '快照：base SHA',
  snapshot_head_sha      varchar(64)   NOT NULL COMMENT '快照：head SHA',
  result_summary         varchar(1000) DEFAULT NULL COMMENT '结果摘要（中文）',
  result_json            mediumtext    DEFAULT NULL COMMENT '结构化结果JSON（限长）',
  duration_ms            bigint(20)    DEFAULT NULL COMMENT '本轮耗时(毫秒)',
  started_time           datetime      NOT NULL COMMENT '开始时间',
  finished_time          datetime      DEFAULT NULL COMMENT '结束时间',
  create_by              varchar(64)   DEFAULT '',
  create_time            datetime      DEFAULT NULL,
  update_by              varchar(64)   DEFAULT '',
  update_time            datetime      DEFAULT NULL,
  PRIMARY KEY (run_id),
  UNIQUE KEY uk_task_attempt (task_id, attempt_no),
  KEY idx_run_task (task_id),
  KEY idx_run_status (run_status),
  KEY idx_run_started (started_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审查任务执行记录';

-- ----------------------------
-- 功能权限：任务重试
-- ----------------------------
INSERT INTO sys_menu SELECT 1139, '任务重试', 125, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:task:retry', '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1139);

INSERT INTO sys_role_menu SELECT '2', '1139'
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1139');

-- ----------------------------
-- 业务字典
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审查方式', 'review_mode', '0', 'admin', SYSDATE(), '项目审查方式'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_mode');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, 'PR 增量审查', 'OCR_PR_DIFF', 'review_mode', '', 'primary', 'Y', '0', 'admin', SYSDATE(), '基于 PR base/head 的 OCR 增量审查'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_mode' AND dict_value = 'OCR_PR_DIFF');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审查引擎', 'review_engine_code', '0', 'admin', SYSDATE(), '审查引擎编码'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_engine_code');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, 'open-code-review', 'OPEN_CODE_REVIEW', 'review_engine_code', '', 'success', 'Y', '0', 'admin', SYSDATE(), '本地 OCR CLI 引擎'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_engine_code' AND dict_value = 'OPEN_CODE_REVIEW');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审查结论', 'review_conclusion', '0', 'admin', SYSDATE(), '审查任务结论（与执行状态独立）'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_conclusion');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '通过', 'PASS', 'review_conclusion', '', 'success', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_conclusion' AND dict_value = 'PASS');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '警告', 'WARN', 'review_conclusion', '', 'warning', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_conclusion' AND dict_value = 'WARN');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '阻断', 'BLOCK', 'review_conclusion', '', 'danger', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_conclusion' AND dict_value = 'BLOCK');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审查执行步骤', 'review_task_step', '0', 'admin', SYSDATE(), '审查任务执行步骤'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_task_step');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '解析配置', 'RESOLVE_CONFIG', 'review_task_step', '', 'info', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_step' AND dict_value = 'RESOLVE_CONFIG');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '准备工作区', 'PREPARE_WORKSPACE', 'review_task_step', '', 'info', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_step' AND dict_value = 'PREPARE_WORKSPACE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '调用审查引擎', 'INVOKE_ENGINE', 'review_task_step', '', 'primary', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_step' AND dict_value = 'INVOKE_ENGINE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '保存结果', 'PERSIST_RESULT', 'review_task_step', '', 'success', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_step' AND dict_value = 'PERSIST_RESULT');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审查任务失败分类', 'review_task_failure_type', '0', 'admin', SYSDATE(), '审查任务失败分类'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_task_failure_type');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '配置缺失', 'CONFIG_MISSING', 'review_task_failure_type', '', 'warning', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_failure_type' AND dict_value = 'CONFIG_MISSING');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '凭据错误', 'CREDENTIAL_ERROR', 'review_task_failure_type', '', 'danger', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_failure_type' AND dict_value = 'CREDENTIAL_ERROR');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '工作区准备失败', 'WORKSPACE_PREPARE_FAILED', 'review_task_failure_type', '', 'danger', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_failure_type' AND dict_value = 'WORKSPACE_PREPARE_FAILED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '引擎超时', 'TIMEOUT', 'review_task_failure_type', '', 'warning', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_failure_type' AND dict_value = 'TIMEOUT');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '引擎调用失败', 'ENGINE_FAILED', 'review_task_failure_type', '', 'danger', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_failure_type' AND dict_value = 'ENGINE_FAILED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '模型调用失败', 'MODEL_CALL_FAILED', 'review_task_failure_type', '', 'danger', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_failure_type' AND dict_value = 'MODEL_CALL_FAILED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 7, '并发超限', 'CONCURRENCY_LIMIT', 'review_task_failure_type', '', 'warning', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_failure_type' AND dict_value = 'CONCURRENCY_LIMIT');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 8, '未知错误', 'UNKNOWN', 'review_task_failure_type', '', 'info', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_failure_type' AND dict_value = 'UNKNOWN');
