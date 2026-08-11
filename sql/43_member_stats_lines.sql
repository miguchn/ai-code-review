-- ----------------------------
-- 43_member_stats_lines.sql
-- M12 补充：成员日聚合增删行数（additions_sum / deletions_sum）
--
-- 部署边界：须先执行 38_data_insights_m12_2.sql（建表 review_member_stats_daily）。
-- 本脚本幂等可重跑。须 utf8mb4。
-- ----------------------------

DROP PROCEDURE IF EXISTS upgrade_member_stats_lines_m12;
DELIMITER $$
CREATE PROCEDURE upgrade_member_stats_lines_m12()
BEGIN
  IF EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'review_member_stats_daily'
  ) THEN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'review_member_stats_daily'
          AND column_name = 'additions_sum'
    ) THEN
      ALTER TABLE review_member_stats_daily
        ADD COLUMN additions_sum int NOT NULL DEFAULT 0 COMMENT '新增行数合计(SUCCESS且非空)' AFTER tasks_reviewed;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'review_member_stats_daily'
          AND column_name = 'deletions_sum'
    ) THEN
      ALTER TABLE review_member_stats_daily
        ADD COLUMN deletions_sum int NOT NULL DEFAULT 0 COMMENT '删减行数合计(SUCCESS且非空)' AFTER additions_sum;
    END IF;
  END IF;
END$$
DELIMITER ;
CALL upgrade_member_stats_lines_m12();
DROP PROCEDURE IF EXISTS upgrade_member_stats_lines_m12;
