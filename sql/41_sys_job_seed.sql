-- ----------------------------
-- 41_sys_job_seed.sql
-- 基础定时任务种子：数据洞察聚合（随初始化脚本体系交付，用户无需手工注册）
--
-- 背景：M12 数据洞察聚合任务（insightStatsJobTask.refreshRecent / fullRecalc）
-- 原设计为运行期手工注册，落地反馈「太难用」，改为随迁移脚本幂等注册。
-- 幂等可重跑。须 utf8mb4 连接执行。
-- ----------------------------

INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '数据洞察-近期聚合刷新', 'DEFAULT', 'insightStatsJobTask.refreshRecent', '0 */10 * * * ?', '3', '1', '0', 'admin', SYSDATE(), '每10分钟重算昨日+今日数据洞察聚合'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'insightStatsJobTask.refreshRecent');

INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '数据洞察-夜间全量重算', 'DEFAULT', 'insightStatsJobTask.fullRecalc', '0 30 2 * * ?', '3', '1', '0', 'admin', SYSDATE(), '每日凌晨重算近35天数据洞察聚合'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'insightStatsJobTask.fullRecalc');
