-- ----------------------------
-- M5：IM 三渠道通知与投递记录
-- 日期: 2026-08-03
-- 前置: 04_github_project_access.sql、23_review_delivery_record.sql
-- 说明:
--   1) 新建 review_notify_channel：平台级群机器人渠道（钉钉/企微/飞书）
--   2) review_project 增 notify_enabled / notify_channel_id / notify_on_failure
--   3) 字典 review_notify_channel_type、review_delivery_channel；参数 review.ui.base-url
--   4) 一级菜单「通知管理」(5) + 通知渠道(129) + 投递记录(130) 及按钮权限
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/24_notification_management_m5.sql
-- ----------------------------

SET NAMES utf8mb4;

-- ----------------------------
-- 通知渠道表
-- ----------------------------
CREATE TABLE IF NOT EXISTS review_notify_channel (
  channel_id              bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '渠道ID',
  channel_name            varchar(64)   NOT NULL COMMENT '渠道名称',
  channel_type            varchar(40)   NOT NULL COMMENT '渠道类型(DINGTALK_ROBOT/WECOM_ROBOT/FEISHU_BOT)',
  webhook_url_ciphertext  varchar(2000) NOT NULL COMMENT 'AES-GCM 加密后的 Webhook URL',
  secret_ciphertext       varchar(1000) DEFAULT NULL COMMENT 'AES-GCM 加密后的加签 Secret（可空）',
  status                  char(1)       NOT NULL DEFAULT '0' COMMENT '状态(0启用 1停用)',
  last_check_status       varchar(20)   NOT NULL DEFAULT 'UNTESTED' COMMENT '最近检测状态',
  last_check_message      varchar(255)  DEFAULT NULL COMMENT '最近检测结果',
  last_check_time         datetime      DEFAULT NULL COMMENT '最近检测时间',
  remark                  varchar(500)  DEFAULT '',
  create_by               varchar(64)   DEFAULT '',
  create_time             datetime      DEFAULT NULL,
  update_by               varchar(64)   DEFAULT '',
  update_time             datetime      DEFAULT NULL,
  PRIMARY KEY (channel_id),
  UNIQUE KEY uk_notify_channel_type_name (channel_type, channel_name),
  KEY idx_notify_channel_status (status),
  KEY idx_notify_channel_type (channel_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审查通知渠道（群机器人）';

-- ----------------------------
-- 项目通知绑定列
-- ----------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'notify_enabled'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE review_project ADD COLUMN notify_enabled char(1) NOT NULL DEFAULT ''N'' COMMENT ''是否启用 IM 通知(Y/N)'' AFTER scope_expand_enabled',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'notify_channel_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE review_project ADD COLUMN notify_channel_id bigint(20) DEFAULT NULL COMMENT ''通知渠道ID'' AFTER notify_enabled',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'notify_on_failure'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE review_project ADD COLUMN notify_on_failure char(1) NOT NULL DEFAULT ''Y'' COMMENT ''FAILED 时是否发送简讯(Y/N)'' AFTER notify_channel_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE table_schema = DATABASE() AND table_name = 'review_project' AND index_name = 'idx_review_project_notify_channel'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE review_project ADD KEY idx_review_project_notify_channel (notify_channel_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 字典：通知渠道类型
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '通知渠道类型', 'review_notify_channel_type', '0', 'admin', SYSDATE(), '钉钉/企微/飞书群机器人'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_notify_channel_type');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '钉钉机器人', 'DINGTALK_ROBOT', 'review_notify_channel_type', '', 'primary', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_notify_channel_type' AND dict_value = 'DINGTALK_ROBOT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '企微机器人', 'WECOM_ROBOT', 'review_notify_channel_type', '', 'success', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_notify_channel_type' AND dict_value = 'WECOM_ROBOT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '飞书机器人', 'FEISHU_BOT', 'review_notify_channel_type', '', 'warning', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_notify_channel_type' AND dict_value = 'FEISHU_BOT');

-- ----------------------------
-- 字典：投递渠道（含 GitHub 评论回写）
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审查投递渠道', 'review_delivery_channel', '0', 'admin', SYSDATE(), 'GitHub 评论与 IM 机器人投递渠道'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'review_delivery_channel');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, 'GitHub 总结评论', 'GITHUB_PR_SUMMARY_COMMENT', 'review_delivery_channel', '', 'info', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_channel' AND dict_value = 'GITHUB_PR_SUMMARY_COMMENT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '钉钉机器人', 'DINGTALK_ROBOT', 'review_delivery_channel', '', 'primary', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_channel' AND dict_value = 'DINGTALK_ROBOT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '企微机器人', 'WECOM_ROBOT', 'review_delivery_channel', '', 'success', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_channel' AND dict_value = 'WECOM_ROBOT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '飞书机器人', 'FEISHU_BOT', 'review_delivery_channel', '', 'warning', 'N', '0', 'admin', SYSDATE(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_delivery_channel' AND dict_value = 'FEISHU_BOT');

-- ----------------------------
-- 参数：后台 UI 基址（IM 详情链接）
-- ----------------------------
INSERT INTO sys_config
  (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '审查后台 UI 基址', 'review.ui.base-url', '', 'Y', 'admin', SYSDATE(), '', NULL,
       'IM 消息「详情」链接前缀，如 https://acr.example.com；空则省略详情链'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.ui.base-url');

-- ----------------------------
-- 菜单：通知管理
-- ----------------------------
INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 5, '通知管理', 0, 2, 'notify', NULL, '', '',
       1, 0, 'M', '0', '0', '', 'message',
       'admin', SYSDATE(), '', NULL, '通知管理一级目录（渠道与投递记录）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 5);

UPDATE sys_menu SET order_num = 1, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 3;
UPDATE sys_menu SET order_num = 2, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 5;
UPDATE sys_menu SET order_num = 3, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 4;
UPDATE sys_menu SET order_num = 4, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 1;
UPDATE sys_menu SET order_num = 5, update_by = 'admin', update_time = SYSDATE() WHERE menu_id = 2;

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 129, '通知渠道', 5, 1, 'channel', 'notify/channel/index', '', '',
       1, 0, 'C', '0', '0', 'review:notify:list', 'channel',
       'admin', SYSDATE(), '', NULL, '钉钉/企微/飞书群机器人渠道'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 129);

-- 已执行过本脚本的环境：通知渠道菜单图标由 guide（不存在）修正为 channel
UPDATE sys_menu SET icon = 'channel', update_by = 'admin', update_time = SYSDATE()
WHERE menu_id = 129 AND icon <> 'channel';

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT 130, '投递记录', 5, 2, 'delivery', 'notify/delivery/index', '', '',
       1, 0, 'C', '0', '0', 'review:delivery:list', 'log',
       'admin', SYSDATE(), '', NULL, '审查结果投递记录（含 GitHub 评论回写）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 130);

INSERT INTO sys_menu SELECT 1151, '渠道查询', 129, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:notify:query',  '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1151);
INSERT INTO sys_menu SELECT 1152, '渠道新增', 129, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:notify:add',    '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1152);
INSERT INTO sys_menu SELECT 1153, '渠道修改', 129, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:notify:edit',   '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1153);
INSERT INTO sys_menu SELECT 1154, '渠道删除', 129, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:notify:remove', '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1154);
INSERT INTO sys_menu SELECT 1155, '渠道启停', 129, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:notify:status', '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1155);
INSERT INTO sys_menu SELECT 1156, '测试发送', 129, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:notify:test',   '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1156);
INSERT INTO sys_menu SELECT 1157, '渠道列表', 129, 7, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:notify:list',   '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1157);

INSERT INTO sys_menu SELECT 1158, '投递查询', 130, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:delivery:query', '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1158);
INSERT INTO sys_menu SELECT 1159, '投递列表', 130, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:delivery:list',  '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1159);

INSERT INTO sys_role_menu SELECT '2', '5' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '5');
INSERT INTO sys_role_menu SELECT '2', '129' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '129');
INSERT INTO sys_role_menu SELECT '2', '130' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '130');
INSERT INTO sys_role_menu SELECT '2', '1151' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1151');
INSERT INTO sys_role_menu SELECT '2', '1152' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1152');
INSERT INTO sys_role_menu SELECT '2', '1153' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1153');
INSERT INTO sys_role_menu SELECT '2', '1154' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1154');
INSERT INTO sys_role_menu SELECT '2', '1155' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1155');
INSERT INTO sys_role_menu SELECT '2', '1156' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1156');
INSERT INTO sys_role_menu SELECT '2', '1157' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1157');
INSERT INTO sys_role_menu SELECT '2', '1158' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1158');
INSERT INTO sys_role_menu SELECT '2', '1159' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1159');
