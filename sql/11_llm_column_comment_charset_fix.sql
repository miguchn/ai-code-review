-- ----------------------------
-- 修复 custom_provider_name 列注释乱码
-- 日期: 2026-08-01
-- 前置: 09_llm_custom_provider.sql
-- 说明: 09 在未指定 utf8mb4 连接字符集时执行，导致列 COMMENT 双重编码；
--       本脚本仅重写列注释，不改动列类型与默认值。
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/11_llm_column_comment_charset_fix.sql
-- ----------------------------

SET NAMES utf8mb4;

ALTER TABLE sys_ai_model_config
  MODIFY COLUMN custom_provider_name varchar(64) COLLATE utf8mb4_general_ci DEFAULT ''
  COMMENT '自定义厂商名称(provider=custom 时使用)';
