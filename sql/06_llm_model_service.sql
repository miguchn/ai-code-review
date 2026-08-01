-- ----------------------------
-- LLM 模型服务增量脚本
-- 日期: 2026-08-01
-- 前置: 01_core_schema.sql、02_quartz_schema.sql、03_system_management.sql
-- 说明: sys_ai_model_config 扩展字段；新增「模型服务」一级目录并将 AI 大模型配置菜单迁入。
-- ----------------------------

DROP PROCEDURE IF EXISTS upgrade_llm_model_service;
DELIMITER $$
CREATE PROCEDURE upgrade_llm_model_service()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'sys_ai_model_config' AND column_name = 'temperature'
  ) THEN
    ALTER TABLE sys_ai_model_config
      ADD COLUMN temperature double DEFAULT NULL COMMENT 'Temperature' AFTER max_tokens;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'sys_ai_model_config' AND column_name = 'context_length'
  ) THEN
    ALTER TABLE sys_ai_model_config
      ADD COLUMN context_length int DEFAULT NULL COMMENT '上下文长度' AFTER temperature;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'sys_ai_model_config' AND column_name = 'last_check_result'
  ) THEN
    ALTER TABLE sys_ai_model_config
      ADD COLUMN last_check_result varchar(500) DEFAULT '' COMMENT '最近检测结果' AFTER context_length;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'sys_ai_model_config' AND column_name = 'last_check_time'
  ) THEN
    ALTER TABLE sys_ai_model_config
      ADD COLUMN last_check_time datetime DEFAULT NULL COMMENT '最近检测时间' AFTER last_check_result;
  END IF;

  UPDATE sys_ai_model_config config
  JOIN (
      SELECT MIN(model_id) AS keep_model_id
      FROM sys_ai_model_config
      WHERE is_default = '1'
  ) defaults ON defaults.keep_model_id IS NOT NULL
  SET config.is_default = '0'
  WHERE config.is_default = '1' AND config.model_id <> defaults.keep_model_id;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'sys_ai_model_config' AND column_name = 'default_slot'
  ) THEN
    ALTER TABLE sys_ai_model_config
      ADD COLUMN default_slot tinyint
        GENERATED ALWAYS AS (IF(is_default = '1', 1, NULL)) STORED
        COMMENT '默认模型唯一槽位' AFTER is_default;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'sys_ai_model_config'
        AND index_name = 'uk_ai_model_default_slot'
  ) THEN
    ALTER TABLE sys_ai_model_config
      ADD UNIQUE KEY uk_ai_model_default_slot (default_slot);
  END IF;
END$$
DELIMITER ;
CALL upgrade_llm_model_service();
DROP PROCEDURE IF EXISTS upgrade_llm_model_service;

-- 兼容旧版厂商编码：Anthropic 归一为 Claude，自定义 OpenAI 兼容配置归一为 OpenAI。
UPDATE sys_ai_model_config SET provider = 'claude' WHERE LOWER(TRIM(provider)) = 'anthropic';
UPDATE sys_ai_model_config SET provider = 'openai' WHERE LOWER(TRIM(provider)) IN ('custom', 'gpt');

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 4, '模型服务', 0, 2, 'model-service', NULL, '', '',
       1, 0, 'M', '0', '0', '', 'server',
       'admin', SYSDATE(), '', NULL, '模型服务一级目录（位于代码审查之后、系统管理之前）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4);

UPDATE sys_menu
SET order_num = 2,
    update_by = 'admin',
    update_time = SYSDATE()
WHERE menu_id = 4;

UPDATE sys_menu SET order_num = 1, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 3;
UPDATE sys_menu SET order_num = 3, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 1;
UPDATE sys_menu SET order_num = 4, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 2;

UPDATE sys_menu
SET menu_name = '大模型配置',
    parent_id = 4,
    order_num = 1,
    path = 'ai-model-config',
    component = 'system/aimodelconfig/index',
    update_by = 'admin',
    update_time = SYSDATE(),
    remark = '模型服务 - 大模型配置'
WHERE menu_id = 120;

INSERT INTO sys_role_menu SELECT '2', '4' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '4');
