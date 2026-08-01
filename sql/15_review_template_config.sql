-- ----------------------------
-- 项目审查模板公共配置
-- 日期: 2026-08-01
-- 前置: 14_review_dual_mode_prompt.sql
-- 说明:
--   1) review_prompt 升级为 review_template（语言栈/版本/内置标识）
--   2) 项目增加 primary_stack、prompt_id→template_id
--   3) 任务建单快照字段；run 快照对齐模板命名
--   4) 菜单迁至「代码审查 → 审查模板」，下线模型服务下提示词菜单
-- ----------------------------

-- ----------------------------
-- 1. 表升级：review_prompt → review_template
-- ----------------------------
DROP PROCEDURE IF EXISTS upgrade_review_template_table;
DELIMITER $$
CREATE PROCEDURE upgrade_review_template_table()
BEGIN
  IF EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'review_prompt'
  ) AND NOT EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'review_template'
  ) THEN
    RENAME TABLE review_prompt TO review_template;
  END IF;

  IF EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'review_template'
  ) THEN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'review_template' AND column_name = 'prompt_id'
    ) THEN
      ALTER TABLE review_template
        CHANGE COLUMN prompt_id template_id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '模板ID',
        CHANGE COLUMN prompt_name template_name varchar(100) NOT NULL COMMENT '模板名称',
        CHANGE COLUMN prompt_code template_code varchar(64) NOT NULL COMMENT '模板编码';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'review_template' AND column_name = 'tech_stack'
    ) THEN
      ALTER TABLE review_template
        ADD COLUMN tech_stack varchar(40) NOT NULL DEFAULT 'FULLSTACK' COMMENT '适用技术栈' AFTER template_code,
        ADD COLUMN version_no int(11) NOT NULL DEFAULT 1 COMMENT '模板版本号' AFTER content,
        ADD COLUMN builtin_flag char(1) NOT NULL DEFAULT '0' COMMENT '是否内置(1是 0否)' AFTER version_no;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'review_template' AND index_name = 'idx_review_template_stack_status'
    ) THEN
      ALTER TABLE review_template ADD KEY idx_review_template_stack_status (tech_stack, status);
    END IF;
  END IF;
END$$
DELIMITER ;
CALL upgrade_review_template_table();
DROP PROCEDURE IF EXISTS upgrade_review_template_table;

-- 历史默认提示词标记为全栈内置
UPDATE review_template
SET template_code = 'builtin_fullstack',
    template_name = '全栈通用',
    tech_stack = 'FULLSTACK',
    builtin_flag = '1',
    version_no = IFNULL(version_no, 1)
WHERE template_code IN ('default_pr_review', 'builtin_fullstack');

-- ----------------------------
-- 2. 内置模板种子
-- ----------------------------
DROP PROCEDURE IF EXISTS seed_review_template;
DELIMITER $$
CREATE PROCEDURE seed_review_template(
  IN p_code varchar(64),
  IN p_name varchar(100),
  IN p_stack varchar(40),
  IN p_content mediumtext
)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM review_template WHERE template_code = p_code) THEN
    INSERT INTO review_template
      (template_name, template_code, tech_stack, content, version_no, builtin_flag, status, remark, create_by, create_time)
    VALUES
      (p_name, p_code, p_stack, p_content, 1, '1', '0', '系统内置审查模板，请复制后按项目调整', 'admin', SYSDATE());
  END IF;
END$$
DELIMITER ;

CALL seed_review_template('builtin_java', 'Java', 'JAVA',
'你是资深 Java 代码审查助手。请基于以下 Pull Request 变更进行审查，重点关注：空指针与异常处理、并发安全、资源关闭、SQL/注入风险、Spring 事务与分层边界、集合与流式 API 误用、明显性能问题。

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

请用中文输出可执行建议。若可能，按 JSON 返回：
{"summary":"一句话总结","conclusion":"PASS|WARN|BLOCK","comments":[{"severity":"critical|warning|info","file":"路径","line":1,"message":"问题说明","suggestion":"修复建议"}]}
不要编造未在 Diff 中出现的文件或行号。');

CALL seed_review_template('builtin_python', 'Python', 'PYTHON',
'你是资深 Python 代码审查助手。请基于以下 Pull Request 变更进行审查，重点关注：异常处理与资源管理、类型注解一致性、可变默认参数、并发/异步误用、注入与反序列化风险、测试可维护性、明显性能问题。

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

请用中文输出可执行建议。若可能，按 JSON 返回：
{"summary":"一句话总结","conclusion":"PASS|WARN|BLOCK","comments":[{"severity":"critical|warning|info","file":"路径","line":1,"message":"问题说明","suggestion":"修复建议"}]}
不要编造未在 Diff 中出现的文件或行号。');

CALL seed_review_template('builtin_go', 'Go', 'GO',
'你是资深 Go 代码审查助手。请基于以下 Pull Request 变更进行审查，重点关注：error 处理、goroutine/channel 泄漏与竞态、上下文传递、接口边界、资源关闭、SQL 注入、明显性能问题。

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

请用中文输出可执行建议。若可能，按 JSON 返回：
{"summary":"一句话总结","conclusion":"PASS|WARN|BLOCK","comments":[{"severity":"critical|warning|info","file":"路径","line":1,"message":"问题说明","suggestion":"修复建议"}]}
不要编造未在 Diff 中出现的文件或行号。');

CALL seed_review_template('builtin_vue', 'Vue', 'VUE',
'你是资深 Vue 前端代码审查助手。请基于以下 Pull Request 变更进行审查，重点关注：响应式误用、组件边界与 props/emit、路由与权限、XSS/危险 HTML、状态管理副作用、可访问性与明显性能问题。

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

请用中文输出可执行建议。若可能，按 JSON 返回：
{"summary":"一句话总结","conclusion":"PASS|WARN|BLOCK","comments":[{"severity":"critical|warning|info","file":"路径","line":1,"message":"问题说明","suggestion":"修复建议"}]}
不要编造未在 Diff 中出现的文件或行号。');

CALL seed_review_template('builtin_react', 'React', 'REACT',
'你是资深 React 前端代码审查助手。请基于以下 Pull Request 变更进行审查，重点关注：Hooks 依赖与副作用、状态提升边界、key/列表渲染、XSS/危险 HTML、并发渲染下的竞态、可访问性与明显性能问题。

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

请用中文输出可执行建议。若可能，按 JSON 返回：
{"summary":"一句话总结","conclusion":"PASS|WARN|BLOCK","comments":[{"severity":"critical|warning|info","file":"路径","line":1,"message":"问题说明","suggestion":"修复建议"}]}
不要编造未在 Diff 中出现的文件或行号。');

CALL seed_review_template('builtin_fullstack', '全栈通用', 'FULLSTACK',
'你是资深代码审查助手。请基于以下 Pull Request 变更进行审查，关注正确性、安全性、可维护性与明显缺陷，覆盖前后端与配置变更的通用风险。

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

请用中文输出可执行建议。若可能，按 JSON 返回：
{"summary":"一句话总结","conclusion":"PASS|WARN|BLOCK","comments":[{"severity":"critical|warning|info","file":"路径","line":1,"message":"问题说明","suggestion":"修复建议"}]}
不要编造未在 Diff 中出现的文件或行号。');

DROP PROCEDURE IF EXISTS seed_review_template;

-- ----------------------------
-- 3. 项目字段：primary_stack + template_id
-- ----------------------------
DROP PROCEDURE IF EXISTS upgrade_review_project_template;
DELIMITER $$
CREATE PROCEDURE upgrade_review_project_template()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'primary_stack'
  ) THEN
    ALTER TABLE review_project
      ADD COLUMN primary_stack varchar(40) DEFAULT NULL COMMENT '项目主要语言/技术栈' AFTER engine_code;
  END IF;

  IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'prompt_id'
  ) AND NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'template_id'
  ) THEN
    ALTER TABLE review_project
      CHANGE COLUMN prompt_id template_id bigint(20) DEFAULT NULL COMMENT '审查模板ID（大模型审查必填）';
  END IF;
END$$
DELIMITER ;
CALL upgrade_review_project_template();
DROP PROCEDURE IF EXISTS upgrade_review_project_template;

UPDATE review_project
SET primary_stack = 'FULLSTACK'
WHERE primary_stack IS NULL OR primary_stack = '';

-- ----------------------------
-- 4. 任务建单快照
-- ----------------------------
DROP PROCEDURE IF EXISTS upgrade_review_task_create_snapshot;
DELIMITER $$
CREATE PROCEDURE upgrade_review_task_create_snapshot()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'snapshot_review_mode'
  ) THEN
    ALTER TABLE review_task
      ADD COLUMN snapshot_review_mode varchar(40) DEFAULT NULL COMMENT '建单快照：审查方式' AFTER duration_ms,
      ADD COLUMN snapshot_template_id bigint(20) DEFAULT NULL COMMENT '建单快照：模板ID' AFTER snapshot_review_mode,
      ADD COLUMN snapshot_template_name varchar(100) DEFAULT NULL COMMENT '建单快照：模板名称' AFTER snapshot_template_id,
      ADD COLUMN snapshot_template_code varchar(64) DEFAULT NULL COMMENT '建单快照：模板编码' AFTER snapshot_template_name,
      ADD COLUMN snapshot_template_version int(11) DEFAULT NULL COMMENT '建单快照：模板版本' AFTER snapshot_template_code,
      ADD COLUMN snapshot_prompt_content mediumtext DEFAULT NULL COMMENT '建单快照：模板正文' AFTER snapshot_template_version,
      ADD COLUMN snapshot_model_id bigint(20) DEFAULT NULL COMMENT '建单快照：模型ID' AFTER snapshot_prompt_content,
      ADD COLUMN snapshot_model_name varchar(100) DEFAULT NULL COMMENT '建单快照：模型名称' AFTER snapshot_model_id,
      ADD COLUMN snapshot_model_provider varchar(40) DEFAULT NULL COMMENT '建单快照：模型厂商' AFTER snapshot_model_name,
      ADD COLUMN snapshot_model varchar(100) DEFAULT NULL COMMENT '建单快照：模型标识' AFTER snapshot_model_provider,
      ADD COLUMN snapshot_engine_code varchar(40) DEFAULT NULL COMMENT '建单快照：引擎编码' AFTER snapshot_model,
      ADD COLUMN snapshot_engine_name varchar(100) DEFAULT NULL COMMENT '建单快照：引擎名称' AFTER snapshot_engine_code;
  END IF;
END$$
DELIMITER ;
CALL upgrade_review_task_create_snapshot();
DROP PROCEDURE IF EXISTS upgrade_review_task_create_snapshot;

-- ----------------------------
-- 5. run 快照：对齐模板命名
-- ----------------------------
DROP PROCEDURE IF EXISTS upgrade_review_task_run_template_snapshot;
DELIMITER $$
CREATE PROCEDURE upgrade_review_task_run_template_snapshot()
BEGIN
  IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task_run' AND column_name = 'snapshot_prompt_id'
  ) AND NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task_run' AND column_name = 'snapshot_template_id'
  ) THEN
    ALTER TABLE review_task_run
      CHANGE COLUMN snapshot_prompt_id snapshot_template_id bigint(20) DEFAULT NULL COMMENT '快照：模板ID',
      CHANGE COLUMN snapshot_prompt_name snapshot_template_name varchar(100) DEFAULT NULL COMMENT '快照：模板名称';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task_run' AND column_name = 'snapshot_template_code'
  ) THEN
    ALTER TABLE review_task_run
      ADD COLUMN snapshot_template_code varchar(64) DEFAULT NULL COMMENT '快照：模板编码' AFTER snapshot_template_name,
      ADD COLUMN snapshot_template_version int(11) DEFAULT NULL COMMENT '快照：模板版本' AFTER snapshot_template_code;
  END IF;
END$$
DELIMITER ;
CALL upgrade_review_task_run_template_snapshot();
DROP PROCEDURE IF EXISTS upgrade_review_task_run_template_snapshot;

-- ----------------------------
-- 6. 字典：技术栈
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审查技术栈', 'review_tech_stack', '0', 'admin', SYSDATE(), '项目主要语言/审查模板适用技术栈'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_tech_stack');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, 'Java', 'JAVA', 'review_tech_stack', '', 'primary', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_tech_stack' AND dict_value = 'JAVA');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, 'Python', 'PYTHON', 'review_tech_stack', '', 'success', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_tech_stack' AND dict_value = 'PYTHON');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, 'Go', 'GO', 'review_tech_stack', '', 'warning', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_tech_stack' AND dict_value = 'GO');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, 'Vue', 'VUE', 'review_tech_stack', '', 'success', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_tech_stack' AND dict_value = 'VUE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, 'React', 'REACT', 'review_tech_stack', '', 'primary', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_tech_stack' AND dict_value = 'REACT');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '全栈通用', 'FULLSTACK', 'review_tech_stack', '', 'info', 'Y', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_tech_stack' AND dict_value = 'FULLSTACK');

-- ----------------------------
-- 7. 菜单：代码审查 → 审查模板；下线旧提示词菜单
-- ----------------------------
UPDATE sys_menu SET order_num = 4, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 123;
UPDATE sys_menu SET order_num = 5, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 125;

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 127, '审查模板', 3, 3, 'template', 'review/template/index', '', 'ReviewTemplate',
       1, 0, 'C', '0', '0', 'review:template:list', 'documentation',
       'admin', SYSDATE(), '', NULL, '项目审查模板管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 127);

INSERT INTO sys_menu SELECT 1144, '模板查询', 127, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:template:query',  '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1144);
INSERT INTO sys_menu SELECT 1145, '模板新增', 127, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:template:add',    '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1145);
INSERT INTO sys_menu SELECT 1146, '模板修改', 127, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:template:edit',   '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1146);
INSERT INTO sys_menu SELECT 1147, '模板删除', 127, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:template:remove', '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1147);
INSERT INTO sys_menu SELECT 1148, '模板复制', 127, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:template:copy',   '#', 'admin', SYSDATE(), '', NULL, '' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1148);

INSERT INTO sys_role_menu SELECT '2', '127' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '127');
INSERT INTO sys_role_menu SELECT '2', '1144' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1144');
INSERT INTO sys_role_menu SELECT '2', '1145' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1145');
INSERT INTO sys_role_menu SELECT '2', '1146' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1146');
INSERT INTO sys_role_menu SELECT '2', '1147' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1147');
INSERT INTO sys_role_menu SELECT '2', '1148' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1148');

-- 下线模型服务下的旧「提示词管理」
UPDATE sys_menu
SET visible = '1', status = '1', update_by = 'admin', update_time = SYSDATE(),
    remark = '已迁移至代码审查 → 审查模板'
WHERE menu_id IN (126, 1140, 1141, 1142, 1143);
