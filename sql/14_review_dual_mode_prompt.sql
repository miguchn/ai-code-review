-- ----------------------------
-- M3 整改：双审查方式 + 提示词管理
-- 日期: 2026-08-01
-- 前置: 13_review_pipeline_m3.sql、06_llm_model_service.sql
-- 说明:
--   1) 审查方式改为互斥二选一：LLM_DIRECT（大模型审查）/ OCR_ENGINE（审查引擎）
--   2) 项目增加 prompt_id；执行快照增加提示词内容
--   3) 新增提示词表、菜单与默认提示词
-- ----------------------------

-- ----------------------------
-- 提示词表
-- ----------------------------
CREATE TABLE IF NOT EXISTS review_prompt (
  prompt_id      bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '提示词ID',
  prompt_name    varchar(100)  NOT NULL COMMENT '提示词名称',
  prompt_code    varchar(64)   NOT NULL COMMENT '提示词编码',
  content        mediumtext    NOT NULL COMMENT '提示词正文（支持占位符）',
  status         char(1)       NOT NULL DEFAULT '0' COMMENT '状态(0启用 1停用)',
  remark         varchar(500)  DEFAULT '',
  create_by      varchar(64)   DEFAULT '',
  create_time    datetime      DEFAULT NULL,
  update_by      varchar(64)   DEFAULT '',
  update_time    datetime      DEFAULT NULL,
  PRIMARY KEY (prompt_id),
  UNIQUE KEY uk_review_prompt_code (prompt_code),
  KEY idx_review_prompt_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审查提示词';

INSERT INTO review_prompt (prompt_name, prompt_code, content, status, remark, create_by, create_time)
SELECT '默认 PR 审查提示词', 'default_pr_review',
'你是资深代码审查助手。请基于以下 Pull Request 变更进行审查，关注正确性、安全性、可维护性与明显缺陷。

【PR 信息】
标题：{{pr_title}}
来源分支：{{source_branch}}
目标分支：{{target_branch}}
Base SHA：{{base_sha}}
Head SHA：{{head_sha}}

【变更 Diff】
```diff
{{diff}}
```

请用中文输出，优先给出可执行建议。若可能，按 JSON 返回：
{"summary":"一句话总结","conclusion":"PASS|WARN|BLOCK","comments":[{"severity":"critical|warning|info","file":"路径","line":1,"message":"问题说明","suggestion":"修复建议"}]}
不要编造未在 Diff 中出现的文件或行号。',
'0', '系统内置默认提示词，可复制后按项目调整', 'admin', SYSDATE()
WHERE NOT EXISTS (SELECT 1 FROM review_prompt WHERE prompt_code = 'default_pr_review');

-- ----------------------------
-- 项目表：提示词绑定 + 审查方式迁移
-- ----------------------------
DROP PROCEDURE IF EXISTS upgrade_review_project_dual_mode;
DELIMITER $$
CREATE PROCEDURE upgrade_review_project_dual_mode()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'prompt_id'
  ) THEN
    ALTER TABLE review_project
      ADD COLUMN prompt_id bigint(20) DEFAULT NULL COMMENT '提示词ID（大模型审查必填）' AFTER model_id;
  END IF;
END$$
DELIMITER ;
CALL upgrade_review_project_dual_mode();
DROP PROCEDURE IF EXISTS upgrade_review_project_dual_mode;

UPDATE review_project
SET review_mode = 'OCR_ENGINE'
WHERE review_mode IS NULL OR review_mode = '' OR review_mode = 'OCR_PR_DIFF';

ALTER TABLE review_project
  MODIFY COLUMN review_mode varchar(40) NOT NULL DEFAULT 'OCR_ENGINE'
  COMMENT '审查方式(LLM_DIRECT=大模型审查, OCR_ENGINE=审查引擎)';

-- 引擎方式不绑定项目级模型/提示词
UPDATE review_project
SET model_id = NULL, prompt_id = NULL
WHERE review_mode = 'OCR_ENGINE';

-- 大模型方式默认引擎编码清空（互斥）
UPDATE review_project
SET engine_code = NULL
WHERE review_mode = 'LLM_DIRECT';

UPDATE review_project
SET engine_code = 'OPEN_CODE_REVIEW'
WHERE review_mode = 'OCR_ENGINE' AND (engine_code IS NULL OR engine_code = '');

-- ----------------------------
-- 执行快照：提示词字段 + 引擎编码可空
-- ----------------------------
DROP PROCEDURE IF EXISTS upgrade_review_task_run_dual_mode;
DELIMITER $$
CREATE PROCEDURE upgrade_review_task_run_dual_mode()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task_run' AND column_name = 'snapshot_prompt_id'
  ) THEN
    ALTER TABLE review_task_run
      ADD COLUMN snapshot_prompt_id bigint(20) DEFAULT NULL COMMENT '快照：提示词ID' AFTER snapshot_model,
      ADD COLUMN snapshot_prompt_name varchar(100) DEFAULT NULL COMMENT '快照：提示词名称' AFTER snapshot_prompt_id,
      ADD COLUMN snapshot_prompt_content mediumtext DEFAULT NULL COMMENT '快照：提示词正文' AFTER snapshot_prompt_name;
  END IF;
END$$
DELIMITER ;
CALL upgrade_review_task_run_dual_mode();
DROP PROCEDURE IF EXISTS upgrade_review_task_run_dual_mode;

UPDATE review_task_run
SET snapshot_review_mode = 'OCR_ENGINE'
WHERE snapshot_review_mode = 'OCR_PR_DIFF';

ALTER TABLE review_task_run
  MODIFY COLUMN snapshot_engine_code varchar(40) DEFAULT NULL COMMENT '快照：引擎编码（大模型审查可空）';

-- ----------------------------
-- 字典：审查方式互斥二选一
-- ----------------------------
UPDATE sys_dict_data
SET dict_label = '审查引擎', dict_value = 'OCR_ENGINE', remark = '调用本机 open-code-review 审查引擎', list_class = 'success'
WHERE dict_type = 'review_mode' AND dict_value = 'OCR_PR_DIFF';

UPDATE sys_dict_data
SET dict_label = '审查引擎', remark = '调用本机 open-code-review 审查引擎', list_class = 'success', is_default = 'Y'
WHERE dict_type = 'review_mode' AND dict_value = 'OCR_ENGINE';

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '大模型审查', 'LLM_DIRECT', 'review_mode', '', 'primary', 'N', '0', 'admin', SYSDATE(), '平台直接调用模型服务与提示词'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_mode' AND dict_value = 'LLM_DIRECT');

UPDATE sys_dict_data SET is_default = 'N'
WHERE dict_type = 'review_mode' AND dict_value = 'LLM_DIRECT';

UPDATE sys_dict_data SET is_default = 'Y', dict_sort = 1
WHERE dict_type = 'review_mode' AND dict_value = 'OCR_ENGINE';

UPDATE sys_dict_data SET dict_sort = 2
WHERE dict_type = 'review_mode' AND dict_value = 'LLM_DIRECT';

-- ----------------------------
-- 菜单：提示词管理（模型服务下）
-- ----------------------------
INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 126, '提示词管理', 4, 3, 'prompt', 'review/prompt/index', '', 'ReviewPrompt',
       1, 0, 'C', '0', '0', 'review:prompt:list', 'edit',
       'admin', SYSDATE(), '', NULL, '审查提示词管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 126);

INSERT INTO sys_menu SELECT 1140, '提示词查询', 126, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:prompt:query',  '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1140);
INSERT INTO sys_menu SELECT 1141, '提示词新增', 126, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:prompt:add',    '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1141);
INSERT INTO sys_menu SELECT 1142, '提示词修改', 126, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:prompt:edit',   '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1142);
INSERT INTO sys_menu SELECT 1143, '提示词删除', 126, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:prompt:remove', '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1143);

INSERT INTO sys_role_menu SELECT '2', '126' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '126');
INSERT INTO sys_role_menu SELECT '2', '1140' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1140');
INSERT INTO sys_role_menu SELECT '2', '1141' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1141');
INSERT INTO sys_role_menu SELECT '2', '1142' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1142');
INSERT INTO sys_role_menu SELECT '2', '1143' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1143');

-- 审查任务：补齐列表/详情权限（历史脚本遗漏会导致详情按钮被权限指令移除）
INSERT INTO sys_role_menu SELECT '2', '125' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '125');
INSERT INTO sys_role_menu SELECT '2', '1135' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1135');
INSERT INTO sys_role_menu SELECT '2', '1139' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1139');

-- ----------------------------
-- 字典：大模型审查步骤
-- ----------------------------
UPDATE sys_dict_data SET dict_sort = 5
WHERE dict_type = 'review_task_step' AND dict_value = 'PERSIST_RESULT';

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '调用大模型', 'INVOKE_MODEL', 'review_task_step', '', 'primary', 'N', '0', 'admin', SYSDATE(), '大模型审查调用模型服务'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_step' AND dict_value = 'INVOKE_MODEL');
