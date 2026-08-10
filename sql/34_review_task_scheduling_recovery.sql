-- ----------------------------
-- 34_review_task_scheduling_recovery.sql
-- 企业级架构风险修复 S2：数据库调度、租约 fencing、重试与宕机恢复
--
-- 部署边界：本脚本必须先于新版本应用执行；当前版本不支持新旧调度协议混跑，
-- 升级时须先停止旧实例。存量 RUNNING 任务保留 30 分钟迁移租约，之后由恢复扫描接管。
-- ----------------------------

DROP PROCEDURE IF EXISTS upgrade_review_task_scheduling;
DELIMITER $$
CREATE PROCEDURE upgrade_review_task_scheduling()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'change_key') THEN
    ALTER TABLE review_task ADD COLUMN change_key varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '逻辑变更键(PR#编号/PUSH#分支)' AFTER task_status;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'next_run_at') THEN
    ALTER TABLE review_task ADD COLUMN next_run_at datetime DEFAULT NULL COMMENT '下次可执行时间(DB时钟)' AFTER change_key;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'lease_owner') THEN
    ALTER TABLE review_task ADD COLUMN lease_owner varchar(128) DEFAULT NULL COMMENT '执行租约持有者' AFTER next_run_at;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'lease_until') THEN
    ALTER TABLE review_task ADD COLUMN lease_until datetime DEFAULT NULL COMMENT '执行租约到期时间(DB时钟)' AFTER lease_owner;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'heartbeat_at') THEN
    ALTER TABLE review_task ADD COLUMN heartbeat_at datetime DEFAULT NULL COMMENT '最近心跳时间(DB时钟)' AFTER lease_until;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'execution_epoch') THEN
    ALTER TABLE review_task ADD COLUMN execution_epoch bigint(20) NOT NULL DEFAULT 0 COMMENT '执行代次(fencing token)' AFTER heartbeat_at;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'retry_count') THEN
    ALTER TABLE review_task ADD COLUMN retry_count int(11) NOT NULL DEFAULT 0 COMMENT '自动重试/恢复次数' AFTER execution_epoch;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'last_error_code') THEN
    ALTER TABLE review_task ADD COLUMN last_error_code varchar(64) DEFAULT NULL COMMENT '最近稳定错误码' AFTER retry_count;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_task' AND column_name = 'superseded_by') THEN
    ALTER TABLE review_task ADD COLUMN superseded_by bigint(20) DEFAULT NULL COMMENT '替代本任务的新任务ID' AFTER last_error_code;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'review_task' AND index_name = 'idx_task_dispatch') THEN
    ALTER TABLE review_task ADD KEY idx_task_dispatch (task_status, next_run_at);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'review_task' AND index_name = 'idx_task_recovery') THEN
    ALTER TABLE review_task ADD KEY idx_task_recovery (task_status, lease_until);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'review_task' AND index_name = 'idx_task_change') THEN
    ALTER TABLE review_task ADD KEY idx_task_change (project_id, change_key, task_id);
  END IF;
END$$
DELIMITER ;
CALL upgrade_review_task_scheduling();
DROP PROCEDURE IF EXISTS upgrade_review_task_scheduling;

-- 存量任务补齐稳定变更键；分支大小写按 utf8mb4_bin 区分。
UPDATE review_task
SET change_key = CASE
  WHEN event_source = 'PUSH' THEN CONCAT('PUSH#', source_branch)
  ELSE CONCAT('PR#', pr_number)
END
WHERE change_key IS NULL OR change_key = '';

ALTER TABLE review_task
  MODIFY COLUMN change_key varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '逻辑变更键(PR#编号/PUSH#分支)',
  MODIFY COLUMN task_status varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT '任务状态(PENDING/RUNNING/RETRYING/SUCCESS/FAILED/CANCELLED/SUPERSEDED)';

UPDATE review_task
SET next_run_at = IFNULL(create_time, SYSDATE())
WHERE task_status IN ('PENDING', 'RETRYING') AND next_run_at IS NULL;

UPDATE review_task
SET lease_owner = 'legacy-migration',
    lease_until = DATE_ADD(SYSDATE(), INTERVAL 30 MINUTE),
    heartbeat_at = SYSDATE()
WHERE task_status = 'RUNNING' AND lease_until IS NULL;

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '待重试', 'RETRYING', 'review_task_status', '', 'warning', 'N', '0', 'admin', SYSDATE(), '依赖暂不可用或执行中断，系统将自动重试'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_status' AND dict_value = 'RETRYING');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 7, '已被替代', 'SUPERSEDED', 'review_task_status', '', 'info', 'N', '0', 'admin', SYSDATE(), '同一变更已有更新版本，本任务仅保留历史'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_status' AND dict_value = 'SUPERSEDED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 11, '依赖暂不可用', 'DEPENDENCY_UNAVAILABLE', 'review_task_failure_type', '', 'warning', 'N', '0', 'admin', SYSDATE(), '网络或临时依赖故障，可自动重试'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_failure_type' AND dict_value = 'DEPENDENCY_UNAVAILABLE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 12, '执行租约过期', 'LEASE_EXPIRED', 'review_task_failure_type', '', 'warning', 'N', '0', 'admin', SYSDATE(), '执行节点中断或失联，由恢复扫描接管'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_failure_type' AND dict_value = 'LEASE_EXPIRED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 13, '执行节点停机', 'WORKER_SHUTDOWN', 'review_task_failure_type', '', 'info', 'N', '0', 'admin', SYSDATE(), '节点停机释放租约，由其他节点接管'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_failure_type' AND dict_value = 'WORKER_SHUTDOWN');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '审查调度-恢复扫描周期(秒)', 'review.task.dispatch.scanIntervalSeconds', '10', 'Y', 'admin', SYSDATE(), '', NULL, '数据库恢复扫描周期，建议 5-15 秒'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.task.dispatch.scanIntervalSeconds');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '审查调度-单次扫描任务数', 'review.task.dispatch.batchSize', '64', 'Y', 'admin', SYSDATE(), '', NULL, '单次按项目公平派发的任务上限'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.task.dispatch.batchSize');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '审查调度-执行租约时长(秒)', 'review.task.lease.seconds', '900', 'Y', 'admin', SYSDATE(), '', NULL, '必须覆盖最长单阶段截止时间，由心跳持续续租'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.task.lease.seconds');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '审查调度-心跳周期(秒)', 'review.task.heartbeat.seconds', '30', 'Y', 'admin', SYSDATE(), '', NULL, '建议不超过租约时长三分之一'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.task.heartbeat.seconds');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '审查调度-自动重试上限', 'review.task.retry.maxAttempts', '3', 'Y', 'admin', SYSDATE(), '', NULL, '达到上限后转已失败并等待人工处置'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.task.retry.maxAttempts');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '审查调度-重试退避基数(秒)', 'review.task.retry.baseDelaySeconds', '30', 'Y', 'admin', SYSDATE(), '', NULL, '指数退避的初始等待时间'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.task.retry.baseDelaySeconds');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '审查调度-重试最大等待(秒)', 'review.task.retry.maxDelaySeconds', '900', 'Y', 'admin', SYSDATE(), '', NULL, '指数退避的等待上限'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.task.retry.maxDelaySeconds');
