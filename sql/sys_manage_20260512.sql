-- ----------------------------
-- AI Code Review 系统管理扩展 - 数据库脚本
-- 日期: 2026-05-12
-- 说明: 新增 AI 大模型配置、业务系统管理相关表、菜单、权限
-- 执行: 在 ry_20260417.sql + quartz.sql 之后执行此脚本
-- ----------------------------

-- ----------------------------
-- 1、AI 大模型配置表
-- ----------------------------
drop table if exists sys_ai_model_config;
create table sys_ai_model_config (
  model_id          bigint(20)      not null auto_increment    comment '模型配置ID',
  model_name        varchar(64)     not null                   comment '模型名称',
  provider          varchar(32)     not null                   comment '模型厂商(openai/anthropic/qwen/deepseek/custom)',
  api_url           varchar(500)    not null                   comment '模型地址',
  api_key           varchar(500)    not null                   comment 'API Key(接入审查前必须加密存储)',
  model             varchar(64)     default ''                 comment 'Model 名称',
  embedding_model   varchar(64)     default ''                 comment 'Embedding Model 名称',
  embedding_api_url varchar(500)    default ''                 comment 'Embedding 接口地址',
  enabled           char(1)         default '0'                comment '是否启用(0否 1是)',
  is_default        char(1)         default '0'                comment '是否默认模型(0否 1是)',
  timeout           int(4)          default 60000              comment '超时时间(ms)',
  max_tokens        int(6)          default 8000               comment '最大 Token 数',
  sort_order        int(4)          default 0                  comment '排序',
  remark            varchar(500)    default '',
  create_by         varchar(64)     default '',
  create_time       datetime,
  update_by         varchar(64)     default '',
  update_time       datetime,
  primary key (model_id)
) engine=innodb auto_increment=100 comment = 'AI 大模型配置表';

-- ----------------------------
-- 2、业务系统表
-- ----------------------------
drop table if exists sys_business_system;
create table sys_business_system (
  system_id         bigint(20)      not null auto_increment    comment '系统ID',
  system_name       varchar(100)    not null                   comment '业务系统名称',
  system_code       varchar(64)     not null                   comment '业务系统编码(唯一)',
  dept_id           bigint(20)      default null               comment '所属部门ID',
  manager_ids       varchar(500)    default ''                 comment '管理用户ID列表(逗号分隔)',
  status            char(1)         default '0'                comment '状态(0正常 1停用)',
  remark            varchar(500)    default '',
  create_by         varchar(64)     default '',
  create_time       datetime,
  update_by         varchar(64)     default '',
  update_time       datetime,
  primary key (system_id),
  unique key uk_system_code (system_code)
) engine=innodb auto_increment=100 comment = '业务系统表';

-- ----------------------------
-- 3、菜单权限 SQL
-- ----------------------------

-- AI 大模型配置 菜单（挂在 系统管理 下，order_num=10）
insert into sys_menu values('120', 'AI大模型配置', '1', '10', 'ai-model-config', 'system/aimodelconfig/index', '', 'SysAiModelConfig', 1, 0, 'C', '0', '0', 'system:aimodelconfig:list', 'ai', 'admin', sysdate(), '', null, 'AI大模型配置菜单');

-- 业务系统管理 菜单（挂在 系统管理 下，order_num=11）
insert into sys_menu values('121', '业务系统管理', '1', '11', 'business-system', 'system/businesssystem/index', '', 'BusinessSystemManage', 1, 0, 'C', '0', '0', 'system:businesssystem:list', 'tree-table', 'admin', sysdate(), '', null, '业务系统管理菜单');

-- AI 大模型配置 按钮权限
insert into sys_menu values('1116', 'AI模型查询', '120', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:aimodelconfig:query',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1117', 'AI模型新增', '120', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:aimodelconfig:add',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1118', 'AI模型修改', '120', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:aimodelconfig:edit',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1119', 'AI模型删除', '120', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:aimodelconfig:remove', '#', 'admin', sysdate(), '', null, '');

-- 业务系统管理 按钮权限
insert into sys_menu values('1120', '业务系统查询', '121', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:businesssystem:query',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1121', '业务系统新增', '121', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:businesssystem:add',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1122', '业务系统修改', '121', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:businesssystem:edit',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1123', '业务系统删除', '121', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:businesssystem:remove', '#', 'admin', sysdate(), '', null, '');

-- ----------------------------
-- 4、角色权限关联（超级管理员自动拥有全部权限，此处补充普通角色）
-- ----------------------------
insert into sys_role_menu values ('2', '120');
insert into sys_role_menu values ('2', '121');
insert into sys_role_menu values ('2', '1116');
insert into sys_role_menu values ('2', '1117');
insert into sys_role_menu values ('2', '1118');
insert into sys_role_menu values ('2', '1119');
insert into sys_role_menu values ('2', '1120');
insert into sys_role_menu values ('2', '1121');
insert into sys_role_menu values ('2', '1122');
insert into sys_role_menu values ('2', '1123');
