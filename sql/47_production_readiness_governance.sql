-- 正式上线前收口：通知最小策略/频控与未交付报告入口治理。
-- 前置: 24_notification_management_m5.sql、37_data_insights_m12.sql
-- 说明:
--   1) 存量项目默认 ALL，保持升级前通知行为；新项目由应用默认 RISK_ONLY。
--   2) 冷却只抑制 PASS/WARN，BLOCK 与 FAILED 始终绕过。
--   3) 报告中心在不可变快照、导出和订阅真正交付前隐藏并停用。

DROP PROCEDURE IF EXISTS upgrade_review_notification_policy;
DELIMITER $$
CREATE PROCEDURE upgrade_review_notification_policy()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'review_project'
      AND column_name = 'notify_result_policy'
  ) THEN
    ALTER TABLE review_project
      ADD COLUMN notify_result_policy varchar(20) NOT NULL DEFAULT 'ALL'
        COMMENT 'SUCCESS 结果通知策略(ALL/RISK_ONLY/BLOCK_ONLY)'
        AFTER notify_on_failure;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'review_project'
      AND column_name = 'notify_cooldown_minutes'
  ) THEN
    ALTER TABLE review_project
      ADD COLUMN notify_cooldown_minutes int NOT NULL DEFAULT 0
        COMMENT '低优先级通知冷却分钟数(0-1440，BLOCK/FAILED绕过)'
        AFTER notify_result_policy;
  END IF;
END$$
DELIMITER ;

CALL upgrade_review_notification_policy();
DROP PROCEDURE IF EXISTS upgrade_review_notification_policy;

-- 新增列时先以 ALL 回填存量，再将后续直接插入的数据库默认值收紧为 RISK_ONLY。
ALTER TABLE review_project
  MODIFY COLUMN notify_result_policy varchar(20) NOT NULL DEFAULT 'RISK_ONLY'
    COMMENT 'SUCCESS 结果通知策略(ALL/RISK_ONLY/BLOCK_ONLY)';

UPDATE sys_menu
SET visible = '1',
    status = '1',
    update_by = 'system',
    update_time = SYSDATE(),
    remark = '企业版规划项：不可变报告、导出与订阅交付后再开放'
WHERE menu_id = 134
  AND component = 'insight/report/placeholder';

UPDATE sys_menu
SET remark = '数据洞察：总览看板、项目分析、成员分析',
    update_by = 'system',
    update_time = SYSDATE()
WHERE menu_id = 7;
