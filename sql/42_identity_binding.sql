-- ----------------------------
-- 42_identity_binding.sql
-- M12 身份关联：sys_user_identity + 存量 claim 迁移 + 管理权限
--
-- 部署边界：须先执行 38_data_insights_m12_2.sql（含 review_insight_identity_claim）。
-- 本脚本幂等可重跑。须 utf8mb4。
-- 迁移边界：同一 user_id 下仅大小写不同的 author_email 先按
-- LOWER(TRIM(author_email)) 分组取 MIN(id)，避免同语句内撞 uk_identity。
-- ----------------------------

DROP PROCEDURE IF EXISTS upgrade_identity_binding_m12;
DELIMITER $$
CREATE PROCEDURE upgrade_identity_binding_m12()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'sys_user_identity'
  ) THEN
    CREATE TABLE sys_user_identity (
      id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
      user_id bigint NOT NULL COMMENT '平台用户ID',
      identity_type varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '身份类型(GIT_COMMIT/IM_WECOM/IM_DINGTALK/IM_FEISHU)',
      identifier varchar(320) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '身份标识(GIT=提交邮箱或名称；IM=账号ID)',
      display_name varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '展示名',
      origin varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SELF' COMMENT '关联来源(SELF/AUTO/ADMIN)',
      create_by varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '',
      create_time datetime DEFAULT NULL,
      PRIMARY KEY (id),
      UNIQUE KEY uk_identity (identity_type, identifier),
      KEY idx_identity_user (user_id, identity_type)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户身份关联';
  END IF;

  -- 存量认领迁移：按 user_id+LOWER(email) 去重取最早一条；全局 identifier 判重保留；旧表保留
  IF EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'review_insight_identity_claim'
  ) THEN
    INSERT INTO sys_user_identity (user_id, identity_type, identifier, display_name, origin, create_by, create_time)
    SELECT c.user_id,
           'GIT_COMMIT',
           LOWER(TRIM(c.author_email)),
           c.author_name,
           'SELF',
           IFNULL(c.create_by, ''),
           c.create_time
    FROM review_insight_identity_claim c
    INNER JOIN (
      SELECT user_id,
             LOWER(TRIM(author_email)) AS identifier,
             MIN(id) AS min_id
      FROM review_insight_identity_claim
      WHERE author_email IS NOT NULL AND TRIM(author_email) != ''
      GROUP BY user_id, LOWER(TRIM(author_email))
    ) d ON c.id = d.min_id
    WHERE NOT EXISTS (
        SELECT 1 FROM sys_user_identity i
        WHERE i.user_id = c.user_id
          AND i.identity_type = 'GIT_COMMIT'
          AND i.identifier = LOWER(TRIM(c.author_email))
      )
      AND NOT EXISTS (
        SELECT 1 FROM sys_user_identity i2
        WHERE i2.identity_type = 'GIT_COMMIT'
          AND i2.identifier = LOWER(TRIM(c.author_email))
      );
  END IF;
END$$
DELIMITER ;
CALL upgrade_identity_binding_m12();
DROP PROCEDURE IF EXISTS upgrade_identity_binding_m12;

-- 成员身份管理按钮（挂成员分析 135；1172 已占用，用 1176）
INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 1176, '成员身份管理', 135, 2, '#', '', '', '',
       1, 0, 'F', '0', '0', 'insight:identity:manage', '#',
       'admin', SYSDATE(), '', NULL, '指派/改派/解除提交邮箱关联'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1176);

INSERT INTO sys_role_menu SELECT '2', '1176'
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1176');
