-- ----------------------------
-- 大模型配置：自定义厂商与菜单文案统一
-- 日期: 2026-08-01
-- 前置: 01_core_schema.sql ~ 08_github_pr_webhook.sql
-- 说明: 新增 custom_provider_name；按钮菜单文案去掉 AI 前缀；权限标识保持不变。
-- ----------------------------

DROP PROCEDURE IF EXISTS upgrade_llm_custom_provider;
DELIMITER $$
CREATE PROCEDURE upgrade_llm_custom_provider()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'sys_ai_model_config'
        AND column_name = 'custom_provider_name'
  ) THEN
    ALTER TABLE sys_ai_model_config
      ADD COLUMN custom_provider_name varchar(64) DEFAULT '' COMMENT '自定义厂商名称(provider=custom 时使用)'
      AFTER provider;
  END IF;
END$$
DELIMITER ;
CALL upgrade_llm_custom_provider();
DROP PROCEDURE IF EXISTS upgrade_llm_custom_provider;

UPDATE sys_menu
SET menu_name = '大模型配置',
    remark = '模型服务 - 大模型配置',
    update_by = 'admin',
    update_time = SYSDATE()
WHERE menu_id = 120;

UPDATE sys_menu SET menu_name = '模型查询', update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 1116;
UPDATE sys_menu SET menu_name = '模型新增', update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 1117;
UPDATE sys_menu SET menu_name = '模型修改', update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 1118;
UPDATE sys_menu SET menu_name = '模型删除', update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 1119;
