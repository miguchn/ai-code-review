-- ----------------------------
-- 39_review_resource_budget.sql
-- 企业级架构风险修复 S5：有界调度与资源预算（R2/R5）
-- 参数进入 sys_config，运行期可调；线程池容量在应用启动时读取。
-- 须 utf8mb4 连接执行。
-- ----------------------------

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '审查调度-专用执行池线程数', 'review.task.executor.poolSize', '4', 'Y', 'admin', SYSDATE(), '', NULL, '审查任务专用有界执行池线程数，启动时生效，不与通用异步池共池'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.task.executor.poolSize');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '审查调度-专用执行池队列容量', 'review.task.executor.queueCapacity', '64', 'Y', 'admin', SYSDATE(), '', NULL, '审查任务专用有界队列容量；满时延迟 next_run_at，禁止 CallerRuns'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.task.executor.queueCapacity');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '审查调度-单项目并发上限', 'review.task.project.maxConcurrency', '2', 'Y', 'admin', SYSDATE(), '', NULL, '同一项目同时进入执行池的任务数上限，配合公平轮询避免单仓库霸占'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.task.project.maxConcurrency');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '审查预算-工作区数量上限', 'review.task.budget.workspace.maxCount', '4', 'Y', 'admin', SYSDATE(), '', NULL, '同时存活的 Git 工作区数量上限，须在准备工作区前获取'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.task.budget.workspace.maxCount');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '审查预算-工作区磁盘上限(MB)', 'review.task.budget.workspace.maxDiskMb', '10240', 'Y', 'admin', SYSDATE(), '', NULL, '工作区根目录磁盘占用上限（MB），超限时任务回队待重试'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.task.budget.workspace.maxDiskMb');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '审查预算-OCR并发上限', 'review.task.budget.ocr.maxConcurrency', '2', 'Y', 'admin', SYSDATE(), '', NULL, 'OCR 外部进程全局并发上限；抢不到名额回 RETRYING，不置 FAILED'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.task.budget.ocr.maxConcurrency');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '审查预算-LLM并发上限', 'review.task.budget.llm.maxConcurrency', '4', 'Y', 'admin', SYSDATE(), '', NULL, '大模型调用全局并发上限；超限时排队退避，不越限调用'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.task.budget.llm.maxConcurrency');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '投递调度-专用执行池线程数', 'review.delivery.executor.poolSize', '2', 'Y', 'admin', SYSDATE(), '', NULL, '投递专用执行池线程数，与审查主池隔离，启动时生效'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.delivery.executor.poolSize');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '投递调度-专用执行池队列容量', 'review.delivery.executor.queueCapacity', '64', 'Y', 'admin', SYSDATE(), '', NULL, '投递专用有界队列容量，满时释放租约并由扫描恢复'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.delivery.executor.queueCapacity');
