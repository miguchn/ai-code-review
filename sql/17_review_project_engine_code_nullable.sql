-- ----------------------------
-- 17 大模型审查方式下项目引擎编码可空
-- 前置：14_review_dual_mode_prompt.sql
-- 说明：LLM_DIRECT 与 OCR_ENGINE 互斥，大模型方式不绑定 engine_code；
--       14 脚本曾将 LLM 项目的 engine_code 置空，但未放宽列约束，导致保存失败。
-- ----------------------------

ALTER TABLE review_project
  MODIFY COLUMN engine_code varchar(40) DEFAULT NULL
  COMMENT '审查引擎编码（仅 OCR_ENGINE 必填；LLM_DIRECT 可空）';

UPDATE review_project
SET engine_code = NULL
WHERE review_mode = 'LLM_DIRECT';

UPDATE review_project
SET engine_code = 'OPEN_CODE_REVIEW'
WHERE review_mode = 'OCR_ENGINE' AND (engine_code IS NULL OR engine_code = '');
