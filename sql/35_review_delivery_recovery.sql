-- ----------------------------
-- 35_review_delivery_recovery.sql
-- 企业级架构风险修复 S3：持久化投递意图、租约领取、自动退避与人工处置
--
-- 部署边界：必须先执行本脚本再启动新版本应用。升级时先停止旧实例，避免旧版本
-- 同步投递路径与新版本异步投递状态机同时写入。存量 FAILED 记录转 MANUAL，防止
-- 升级后自动向外部渠道补发历史消息。
-- ----------------------------

DROP PROCEDURE IF EXISTS upgrade_review_delivery_recovery;
DELIMITER $$
CREATE PROCEDURE upgrade_review_delivery_recovery()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_delivery_record' AND column_name = 'next_attempt_at') THEN
    ALTER TABLE review_delivery_record ADD COLUMN next_attempt_at datetime DEFAULT NULL COMMENT '下次可投递时间(DB时钟)' AFTER attempt_count;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_delivery_record' AND column_name = 'last_error_code') THEN
    ALTER TABLE review_delivery_record ADD COLUMN last_error_code varchar(64) DEFAULT NULL COMMENT '最近稳定错误码' AFTER next_attempt_at;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_delivery_record' AND column_name = 'lease_owner') THEN
    ALTER TABLE review_delivery_record ADD COLUMN lease_owner varchar(128) DEFAULT NULL COMMENT '投递租约持有者' AFTER last_error_code;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'review_delivery_record' AND column_name = 'lease_until') THEN
    ALTER TABLE review_delivery_record ADD COLUMN lease_until datetime DEFAULT NULL COMMENT '投递租约到期时间(DB时钟)' AFTER lease_owner;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'review_delivery_record' AND index_name = 'idx_delivery_pending') THEN
    ALTER TABLE review_delivery_record ADD KEY idx_delivery_pending (delivery_status, next_attempt_at);
  END IF;
END$$
DELIMITER ;
CALL upgrade_review_delivery_recovery();
DROP PROCEDURE IF EXISTS upgrade_review_delivery_recovery;

-- 新状态机允许“尚未尝试”的意图，因此最近尝试时间可空、尝试次数从 0 开始。
ALTER TABLE review_delivery_record
  MODIFY COLUMN delivery_status varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '投递状态(PENDING/SUCCESS/FAILED/MANUAL/SKIPPED)',
  MODIFY COLUMN attempt_count int NOT NULL DEFAULT 0 COMMENT '已完成投递尝试次数',
  MODIFY COLUMN last_attempt_time datetime DEFAULT NULL COMMENT '最近尝试时间';

UPDATE review_delivery_record
SET delivery_status = 'MANUAL',
    next_attempt_at = NULL,
    last_error_code = 'DELIVERY_LEGACY_FAILURE',
    update_by = 'system',
    update_time = SYSDATE()
WHERE delivery_status = 'FAILED' AND next_attempt_at IS NULL;

UPDATE sys_dict_data
SET dict_label = '自动重试中', list_class = 'danger',
    remark = '最近一次投递失败，系统将按下次处理时间自动重试',
    update_by = 'admin', update_time = SYSDATE()
WHERE dict_type = 'review_delivery_status' AND dict_value = 'FAILED';

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '待投递', 'PENDING', 'review_delivery_status', '', 'primary', 'N', '0', 'admin', SYSDATE(), '已持久化投递意图，等待工作节点领取'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_status' AND dict_value = 'PENDING');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '待人工处置', 'MANUAL', 'review_delivery_status', '', 'warning', 'N', '0', 'admin', SYSDATE(), '配置错误或达到自动重试上限'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_status' AND dict_value = 'MANUAL');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '已跳过', 'SKIPPED', 'review_delivery_status', '', 'info', 'N', '0', 'admin', SYSDATE(), '被更新结论替代或由投递围栏抑制'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_status' AND dict_value = 'SKIPPED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 8, '待修复通知配置', 'IM_NOTIFICATION', 'review_delivery_channel', '', 'warning', 'N', '0', 'admin', SYSDATE(), '通知配置不可解析时的人工处置占位渠道'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_channel' AND dict_value = 'IM_NOTIFICATION');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '投递调度-恢复扫描周期(秒)', 'review.delivery.dispatch.scanIntervalSeconds', '10', 'Y', 'admin', SYSDATE(), '', NULL, '数据库投递恢复扫描周期'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.delivery.dispatch.scanIntervalSeconds');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '投递调度-单次扫描记录数', 'review.delivery.dispatch.batchSize', '32', 'Y', 'admin', SYSDATE(), '', NULL, '单次领取的到期投递记录上限'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.delivery.dispatch.batchSize');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '投递调度-执行租约时长(秒)', 'review.delivery.lease.seconds', '120', 'Y', 'admin', SYSDATE(), '', NULL, '须覆盖单次评论或机器人请求截止时间'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.delivery.lease.seconds');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '投递调度-自动尝试上限', 'review.delivery.retry.maxAttempts', '5', 'Y', 'admin', SYSDATE(), '', NULL, '达到上限后转待人工处置'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.delivery.retry.maxAttempts');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '投递调度-重试退避基数(秒)', 'review.delivery.retry.baseDelaySeconds', '30', 'Y', 'admin', SYSDATE(), '', NULL, '指数退避的初始等待时间'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.delivery.retry.baseDelaySeconds');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '投递调度-重试最大等待(秒)', 'review.delivery.retry.maxDelaySeconds', '1800', 'Y', 'admin', SYSDATE(), '', NULL, '指数退避的等待上限'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.delivery.retry.maxDelaySeconds');
