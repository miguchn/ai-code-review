-- 50. Git 平台用户身份映射支持
-- 允许 sys_user_identity.user_id 为空（AUTO 自动发现但未映射的 Git 平台用户）
-- resolver 已处理 user_id=null（ReviewIssueAssigneeResolver.matchIdentity 第 97 行）
ALTER TABLE sys_user_identity MODIFY COLUMN user_id BIGINT NULL COMMENT '系统用户ID；AUTO 发现未映射时为空';
