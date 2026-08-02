-- ----------------------------
-- M3.2 步 3：审查范围项目配置 + 任务快照 + 执行决策快照
-- 日期: 2026-08-02
-- 前置: 13_review_pipeline_m3.sql、19_review_record_experience_m3_1.sql
-- 说明:
--   1) review_project 增加审查范围四列：项目排除 glob、测试文件开关、存量问题开关、高影响扩展开关
--   2) review_task 增加对应四列执行快照：建单/补冻结时随 updateTaskSnapshot 同批落库，执行只读快照
--      快照列可空：M3.2 前已冻结的历史任务保持 NULL，执行层按平台默认（不审测试/不报存量/开启扩展）处理
--   3) review_task_run 增加 scope_decision_json：每次执行的范围决策快照（纳入/排除/扩展/截断及原因），
--      LLM 路径（步 4）与 OCR 路径（步 6）共用本列
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/22_review_scope_config.sql
-- ----------------------------

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS upgrade_review_scope_config_m32;
DELIMITER $$
CREATE PROCEDURE upgrade_review_scope_config_m32()
BEGIN
  -- review_project：审查范围项目配置
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'scope_exclude_patterns'
  ) THEN
    ALTER TABLE review_project
      ADD COLUMN scope_exclude_patterns varchar(2000) DEFAULT NULL COMMENT '审查范围：项目排除路径 glob（换行分隔）' AFTER engine_code,
      ADD COLUMN scope_include_tests char(1) NOT NULL DEFAULT 'N' COMMENT '审查范围：是否审查测试文件(Y启用 N排除)' AFTER scope_exclude_patterns,
      ADD COLUMN scope_report_existing char(1) NOT NULL DEFAULT 'N' COMMENT '审查范围：是否上报历史存量问题(Y保留并标注 N剔除)' AFTER scope_include_tests,
      ADD COLUMN scope_expand_enabled char(1) NOT NULL DEFAULT 'Y' COMMENT '审查范围：高影响变更自动扩展整文件(Y启用 N关闭)' AFTER scope_report_existing;
  END IF;

  -- review_task：审查范围执行快照（可空，NULL 表示 M3.2 前冻结，执行层按平台默认处理）
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'snapshot_scope_exclude_patterns'
  ) THEN
    ALTER TABLE review_task
      ADD COLUMN snapshot_scope_exclude_patterns varchar(2000) DEFAULT NULL COMMENT '快照：项目排除路径 glob（换行分隔）' AFTER snapshot_engine_name,
      ADD COLUMN snapshot_scope_include_tests char(1) DEFAULT NULL COMMENT '快照：是否审查测试文件(Y/N)' AFTER snapshot_scope_exclude_patterns,
      ADD COLUMN snapshot_scope_report_existing char(1) DEFAULT NULL COMMENT '快照：是否上报存量问题(Y/N)' AFTER snapshot_scope_include_tests,
      ADD COLUMN snapshot_scope_expand_enabled char(1) DEFAULT NULL COMMENT '快照：高影响扩展(Y/N)' AFTER snapshot_scope_report_existing;
  END IF;

  -- review_task_run：范围决策快照（LLM 与 OCR 两条执行路径共用）
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_task_run' AND column_name = 'scope_decision_json'
  ) THEN
    ALTER TABLE review_task_run
      ADD COLUMN scope_decision_json mediumtext DEFAULT NULL COMMENT '范围决策快照JSON（纳入/排除/扩展/截断及原因）' AFTER result_json;
  END IF;
END$$
DELIMITER ;
CALL upgrade_review_scope_config_m32();
DROP PROCEDURE IF EXISTS upgrade_review_scope_config_m32;
