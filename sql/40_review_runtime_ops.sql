-- ----------------------------
-- 40_review_runtime_ops.sql
-- 企业级架构风险修复 S6：可观测与运营面（R8）
-- 告警阈值 / 优雅停机参数 / 运行概览菜单与处置权限；须 utf8mb4 连接执行。
-- ----------------------------

-- 告警与停机参数（运行期可调）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '运行告警-超龄PENDING分钟数', 'review.runtime.alert.pendingAgeMinutes', '30', 'Y', 'admin', SYSDATE(), '', NULL, '待执行任务创建超过该分钟数则告警'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.runtime.alert.pendingAgeMinutes');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '运行告警-待投递超龄分钟数', 'review.runtime.alert.deliveryPendingAgeMinutes', '20', 'Y', 'admin', SYSDATE(), '', NULL, '待投递记录创建超过该分钟数则告警'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.runtime.alert.deliveryPendingAgeMinutes');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '运行告警-预算持续饱和分钟数', 'review.runtime.alert.budgetSaturatedMinutes', '10', 'Y', 'admin', SYSDATE(), '', NULL, '工作区/OCR/LLM 任一预算持续打满超过该分钟数则告警'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.runtime.alert.budgetSaturatedMinutes');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '运行告警-失败率窗口分钟数', 'review.runtime.alert.failureRateWindowMinutes', '60', 'Y', 'admin', SYSDATE(), '', NULL, '计算终态失败率的时间窗口'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.runtime.alert.failureRateWindowMinutes');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '运行告警-失败率阈值百分比', 'review.runtime.alert.failureRatePercent', '40', 'Y', 'admin', SYSDATE(), '', NULL, '窗口内 FAILED/(SUCCESS+FAILED+CANCELLED) 超过该百分比则告警'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.runtime.alert.failureRatePercent');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '运行告警-判定周期(秒)', 'review.runtime.alert.scanIntervalSeconds', '30', 'Y', 'admin', SYSDATE(), '', NULL, '内置告警规则周期判定间隔'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.runtime.alert.scanIntervalSeconds');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '优雅停机-排空等待秒数', 'review.runtime.drain.timeoutSeconds', '60', 'Y', 'admin', SYSDATE(), '', NULL, '停机时等待租约内任务完成的最长时间；超时后将 lease_until 置过期由恢复扫描接管'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.runtime.drain.timeoutSeconds');

-- 任务状态「已取消」幂等补齐（08/34 已含，升级环境可重复执行）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '已取消', 'CANCELLED', 'review_task_status', '', 'warning', 'N', '0', 'admin', SYSDATE(), '人工终止，不再被调度领取'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'review_task_status' AND dict_value = 'CANCELLED');

-- 系统监控下「运行概览」
INSERT INTO sys_menu SELECT 136, '运行概览', 2, 7, 'runtime', 'monitor/runtime/index', '', 'ReviewRuntimeOverview',
       1, 0, 'C', '0', '0', 'review:runtime:view', 'dashboard', 'admin', SYSDATE(), '', NULL, '审查调度/资源预算/投递队列运行态与告警处置'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 136);

INSERT INTO sys_menu SELECT 1173, '运行概览查看', 136, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:runtime:view', '#', 'admin', SYSDATE(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1173);

INSERT INTO sys_menu SELECT 1174, '任务终止', 125, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:task:cancel', '#', 'admin', SYSDATE(), '', NULL, '将任务置为已取消并触发围栏'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1174);

INSERT INTO sys_menu SELECT 1175, '任务处置', 125, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'review:task:handle', '#', 'admin', SYSDATE(), '', NULL, '积压处置与投递标记人工已处理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1175);

-- 普通角色授权（超级管理员默认全量）
INSERT INTO sys_role_menu SELECT '2', '136' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '136');
INSERT INTO sys_role_menu SELECT '2', '1173' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1173');
INSERT INTO sys_role_menu SELECT '2', '1174' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1174');
INSERT INTO sys_role_menu SELECT '2', '1175' WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = '2' AND menu_id = '1175');
