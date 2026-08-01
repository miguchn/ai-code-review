-- ----------------------------
-- 统一审查评分与结构化结果协议
-- 日期: 2026-08-01
-- 前置: 15_review_template_config.sql
-- 说明:
--   1) review_task_run 增加评分/协议/解析/Prompt 落库字段
--   2) review_task 同步最新一次执行的关键评分摘要
--   3) 字典增加 RESULT_FORMAT_INVALID
--   4) 内置模板正文升级为纯技术栈重点（剥离旧 JSON 输出指令），version_no +1
-- ----------------------------

-- ----------------------------
-- 1. review_task_run：评分与结果协议字段
-- ----------------------------
DROP PROCEDURE IF EXISTS upgrade_review_task_run_scoring;
DELIMITER $$
CREATE PROCEDURE upgrade_review_task_run_scoring()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task_run' AND column_name = 'total_score'
  ) THEN
    ALTER TABLE review_task_run
      ADD COLUMN total_score int(11) DEFAULT NULL COMMENT '总分(0-100)' AFTER result_json,
      ADD COLUMN score_correctness int(11) DEFAULT NULL COMMENT '维度分：正确性与健壮性(0-40)' AFTER total_score,
      ADD COLUMN score_security int(11) DEFAULT NULL COMMENT '维度分：安全性(0-30)' AFTER score_correctness,
      ADD COLUMN score_practice int(11) DEFAULT NULL COMMENT '维度分：最佳实践(0-20)' AFTER score_security,
      ADD COLUMN score_performance int(11) DEFAULT NULL COMMENT '维度分：性能(0-5)' AFTER score_practice,
      ADD COLUMN score_commit_quality int(11) DEFAULT NULL COMMENT '维度分：提交信息质量(0-5)' AFTER score_performance,
      ADD COLUMN protocol_version varchar(16) DEFAULT NULL COMMENT '结果协议版本' AFTER score_commit_quality,
      ADD COLUMN score_weights_json varchar(500) DEFAULT NULL COMMENT '维度满分权重快照JSON' AFTER protocol_version,
      ADD COLUMN score_threshold int(11) DEFAULT NULL COMMENT '预警分数线(预留)' AFTER score_weights_json,
      ADD COLUMN focus_issue_count int(11) DEFAULT NULL COMMENT '重点问题数(0-3)' AFTER score_threshold,
      ADD COLUMN has_critical_security char(1) DEFAULT NULL COMMENT '是否存在严重安全问题(0否1是)' AFTER focus_issue_count,
      ADD COLUMN top_issues_json mediumtext DEFAULT NULL COMMENT 'Top3重点问题JSON' AFTER has_critical_security,
      ADD COLUMN parse_status varchar(20) DEFAULT NULL COMMENT '结果解析状态(SUCCESS/FAILED)' AFTER top_issues_json,
      ADD COLUMN parse_error varchar(500) DEFAULT NULL COMMENT '解析失败原因' AFTER parse_status,
      ADD COLUMN raw_response_excerpt mediumtext DEFAULT NULL COMMENT '模型原始响应摘要(诊断)' AFTER parse_error,
      ADD COLUMN rendered_prompt mediumtext DEFAULT NULL COMMENT '最终完整提示词(截断)' AFTER raw_response_excerpt,
      ADD COLUMN pr_description varchar(4000) DEFAULT NULL COMMENT 'PR描述摘要(截断)' AFTER rendered_prompt,
      ADD COLUMN commit_messages varchar(4000) DEFAULT NULL COMMENT 'Commit Message摘要(截断)' AFTER pr_description;
  END IF;
END$$
DELIMITER ;
CALL upgrade_review_task_run_scoring();
DROP PROCEDURE IF EXISTS upgrade_review_task_run_scoring;

-- ----------------------------
-- 2. review_task：最新执行评分摘要（列表扩展用）
-- ----------------------------
DROP PROCEDURE IF EXISTS upgrade_review_task_scoring_summary;
DELIMITER $$
CREATE PROCEDURE upgrade_review_task_scoring_summary()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'total_score'
  ) THEN
    ALTER TABLE review_task
      ADD COLUMN total_score int(11) DEFAULT NULL COMMENT '最近一次总分(0-100)' AFTER duration_ms,
      ADD COLUMN score_correctness int(11) DEFAULT NULL COMMENT '最近一次维度分：正确性(0-40)' AFTER total_score,
      ADD COLUMN score_security int(11) DEFAULT NULL COMMENT '最近一次维度分：安全性(0-30)' AFTER score_correctness,
      ADD COLUMN score_practice int(11) DEFAULT NULL COMMENT '最近一次维度分：最佳实践(0-20)' AFTER score_security,
      ADD COLUMN score_performance int(11) DEFAULT NULL COMMENT '最近一次维度分：性能(0-5)' AFTER score_practice,
      ADD COLUMN score_commit_quality int(11) DEFAULT NULL COMMENT '最近一次维度分：提交信息(0-5)' AFTER score_performance,
      ADD COLUMN protocol_version varchar(16) DEFAULT NULL COMMENT '最近一次结果协议版本' AFTER score_commit_quality,
      ADD COLUMN focus_issue_count int(11) DEFAULT NULL COMMENT '最近一次重点问题数(0-3)' AFTER protocol_version,
      ADD COLUMN has_critical_security char(1) DEFAULT NULL COMMENT '最近一次是否存在严重安全问题(0否1是)' AFTER focus_issue_count,
      ADD COLUMN parse_status varchar(20) DEFAULT NULL COMMENT '最近一次结果解析状态' AFTER has_critical_security;
  END IF;
END$$
DELIMITER ;
CALL upgrade_review_task_scoring_summary();
DROP PROCEDURE IF EXISTS upgrade_review_task_scoring_summary;

-- ----------------------------
-- 3. 字典：结果格式异常
-- ----------------------------
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 9, '结果格式异常', 'RESULT_FORMAT_INVALID', 'review_task_failure_type', '', 'danger', 'N', '0', 'admin', SYSDATE(), '模型返回 JSON 解析或校验失败'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_failure_type' AND dict_value = 'RESULT_FORMAT_INVALID');

-- ----------------------------
-- 4. 内置模板：纯技术栈正文 + 新占位符（幂等：仅旧版或未升级时更新并 bump version）
-- ----------------------------
UPDATE review_template
SET content = '你是资深 Java 代码审查助手。请基于以下 Pull Request 变更进行审查，重点关注：空指针与异常处理、并发安全、资源关闭、SQL/注入风险、Spring 事务与分层边界、集合与流式 API 误用、明显性能问题。

【PR 信息】
标题：{{pr_title}}
描述：{{pr_description}}
Commit Message：
{{commit_messages}}
来源分支：{{source_branch}}
目标分支：{{target_branch}}
Base SHA：{{base_sha}}
Head SHA：{{head_sha}}

【变更 Diff】
```diff
{{diff}}
```

不要编造未在 Diff 中出现的文件或行号。',
    version_no = version_no + 1,
    update_by = 'admin',
    update_time = SYSDATE()
WHERE template_code = 'builtin_java'
  AND builtin_flag = '1'
  AND (content LIKE '%按 JSON 返回%' OR content NOT LIKE '%{{pr_description}}%');

UPDATE review_template
SET content = '你是资深 Python 代码审查助手。请基于以下 Pull Request 变更进行审查，重点关注：异常处理与资源管理、类型注解一致性、可变默认参数、并发/异步误用、注入与反序列化风险、测试可维护性、明显性能问题。

【PR 信息】
标题：{{pr_title}}
描述：{{pr_description}}
Commit Message：
{{commit_messages}}
来源分支：{{source_branch}}
目标分支：{{target_branch}}
Base SHA：{{base_sha}}
Head SHA：{{head_sha}}

【变更 Diff】
```diff
{{diff}}
```

不要编造未在 Diff 中出现的文件或行号。',
    version_no = version_no + 1,
    update_by = 'admin',
    update_time = SYSDATE()
WHERE template_code = 'builtin_python'
  AND builtin_flag = '1'
  AND (content LIKE '%按 JSON 返回%' OR content NOT LIKE '%{{pr_description}}%');

UPDATE review_template
SET content = '你是资深 Go 代码审查助手。请基于以下 Pull Request 变更进行审查，重点关注：error 处理、goroutine/channel 泄漏与竞态、上下文传递、接口边界、资源关闭、SQL 注入、明显性能问题。

【PR 信息】
标题：{{pr_title}}
描述：{{pr_description}}
Commit Message：
{{commit_messages}}
来源分支：{{source_branch}}
目标分支：{{target_branch}}
Base SHA：{{base_sha}}
Head SHA：{{head_sha}}

【变更 Diff】
```diff
{{diff}}
```

不要编造未在 Diff 中出现的文件或行号。',
    version_no = version_no + 1,
    update_by = 'admin',
    update_time = SYSDATE()
WHERE template_code = 'builtin_go'
  AND builtin_flag = '1'
  AND (content LIKE '%按 JSON 返回%' OR content NOT LIKE '%{{pr_description}}%');

UPDATE review_template
SET content = '你是资深 Vue 前端代码审查助手。请基于以下 Pull Request 变更进行审查，重点关注：响应式误用、组件边界与 props/emit、路由与权限、XSS/危险 HTML、状态管理副作用、可访问性与明显性能问题。

【PR 信息】
标题：{{pr_title}}
描述：{{pr_description}}
Commit Message：
{{commit_messages}}
来源分支：{{source_branch}}
目标分支：{{target_branch}}
Base SHA：{{base_sha}}
Head SHA：{{head_sha}}

【变更 Diff】
```diff
{{diff}}
```

不要编造未在 Diff 中出现的文件或行号。',
    version_no = version_no + 1,
    update_by = 'admin',
    update_time = SYSDATE()
WHERE template_code = 'builtin_vue'
  AND builtin_flag = '1'
  AND (content LIKE '%按 JSON 返回%' OR content NOT LIKE '%{{pr_description}}%');

UPDATE review_template
SET content = '你是资深 React 前端代码审查助手。请基于以下 Pull Request 变更进行审查，重点关注：Hooks 依赖与副作用、状态提升边界、key/列表渲染、XSS/危险 HTML、并发渲染下的竞态、可访问性与明显性能问题。

【PR 信息】
标题：{{pr_title}}
描述：{{pr_description}}
Commit Message：
{{commit_messages}}
来源分支：{{source_branch}}
目标分支：{{target_branch}}
Base SHA：{{base_sha}}
Head SHA：{{head_sha}}

【变更 Diff】
```diff
{{diff}}
```

不要编造未在 Diff 中出现的文件或行号。',
    version_no = version_no + 1,
    update_by = 'admin',
    update_time = SYSDATE()
WHERE template_code = 'builtin_react'
  AND builtin_flag = '1'
  AND (content LIKE '%按 JSON 返回%' OR content NOT LIKE '%{{pr_description}}%');

UPDATE review_template
SET content = '你是资深代码审查助手。请基于以下 Pull Request 变更进行审查，关注正确性、安全性、可维护性与明显缺陷，覆盖前后端与配置变更的通用风险。

【PR 信息】
标题：{{pr_title}}
描述：{{pr_description}}
Commit Message：
{{commit_messages}}
来源分支：{{source_branch}}
目标分支：{{target_branch}}
Base SHA：{{base_sha}}
Head SHA：{{head_sha}}

【变更 Diff】
```diff
{{diff}}
```

不要编造未在 Diff 中出现的文件或行号。',
    version_no = version_no + 1,
    update_by = 'admin',
    update_time = SYSDATE()
WHERE template_code = 'builtin_fullstack'
  AND builtin_flag = '1'
  AND (content LIKE '%按 JSON 返回%' OR content NOT LIKE '%{{pr_description}}%');
