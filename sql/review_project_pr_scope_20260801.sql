-- ----------------------------
-- P0/M1 GitHub PR 审查范围补充脚本
-- 日期: 2026-08-01
-- 前置: review_project_github_20260801.sql
-- 说明: 增加仓库分支同步状态和 PR 目标分支配置；公共默认值复用系统参数。
-- ----------------------------

DROP PROCEDURE IF EXISTS upgrade_review_project_pr_scope;
DELIMITER $$
CREATE PROCEDURE upgrade_review_project_pr_scope()
BEGIN
  IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'review_branches'
  ) AND NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'pr_target_branches'
  ) THEN
    ALTER TABLE review_project
      CHANGE COLUMN review_branches pr_target_branches varchar(1000) DEFAULT NULL COMMENT 'PR目标分支，逗号分隔';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'pr_review_enabled'
  ) THEN
    ALTER TABLE review_project
      ADD COLUMN pr_review_enabled char(1) NOT NULL DEFAULT '0' COMMENT '是否启用PR审查(0启用 1停用)' AFTER default_branch;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'last_branch_sync_status'
  ) THEN
    ALTER TABLE review_project
      ADD COLUMN last_branch_sync_status varchar(20) NOT NULL DEFAULT 'UNSYNCED' COMMENT '最近分支同步状态' AFTER last_check_time;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'last_branch_sync_message'
  ) THEN
    ALTER TABLE review_project
      ADD COLUMN last_branch_sync_message varchar(500) DEFAULT NULL COMMENT '最近分支同步结果' AFTER last_branch_sync_status;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'review_project' AND column_name = 'last_branch_sync_time'
  ) THEN
    ALTER TABLE review_project
      ADD COLUMN last_branch_sync_time datetime DEFAULT NULL COMMENT '最近分支同步时间' AFTER last_branch_sync_message;
  END IF;

  -- 历史通配配置无法证明来自 GitHub 实际分支，迁移后先关闭 PR 审查，等待项目负责人刷新确认。
  UPDATE review_project
  SET pr_target_branches = NULL,
      pr_review_enabled = '1',
      last_branch_sync_status = 'UNSYNCED',
      last_branch_sync_message = '请读取仓库信息并确认 PR 目标分支',
      last_branch_sync_time = NULL
  WHERE pr_target_branches = '*';
END$$
DELIMITER ;

CALL upgrade_review_project_pr_scope();
DROP PROCEDURE IF EXISTS upgrade_review_project_pr_scope;

INSERT INTO sys_config
  (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '代码审查-GitHub推荐长期分支', 'review.github.longLivedBranches', 'dev,develop,main,int,uat', 'Y',
       'admin', SYSDATE(), '', NULL, '按顺序推荐实际存在的 PR 目标分支'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.github.longLivedBranches');

INSERT INTO sys_config
  (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '代码审查-GitHub机器人分支前缀', 'review.github.robotBranchPrefixes', 'dependabot/,renovate/,github-actions/', 'Y',
       'admin', SYSDATE(), '', NULL, '后续 PR 事件过滤使用，普通项目无需配置'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.github.robotBranchPrefixes');

INSERT INTO sys_config
  (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT '代码审查-GitHub默认PR事件', 'review.github.prEvents', 'opened,reopened,synchronize', 'Y',
       'admin', SYSDATE(), '', NULL, 'MVP 统一启用的 Pull Request 触发事件'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'review.github.prEvents');

UPDATE sys_menu
SET menu_name = '项目连接与同步',
    update_by = 'admin',
    update_time = SYSDATE()
WHERE perms = 'review:project:test';
