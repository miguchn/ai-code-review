# 身份关联设计 v2（系统账号 × Git 提交邮箱 × IM 账号）

- 状态：**定稿待审**（2026-08-11，v2 复核修订）
- 取代：M12 二期「认领」交互（review_insight_identity_claim 的交互形态）
- v2 修订要点：用户文案人话化、引导入口移到痛点处、手动添加为主路径、
  补冲突 UX、部门负责人可指派、旧认领迁移方案明确

## 0. 用户故事（设计锚点）

> 开发者老王用 `wangwei@corp.com` 提交代码。他登录平台，在成员分析里看到「关联你的提交邮箱，
> 即可查看你的提交趋势与被审查情况」，点进去添加了这个邮箱。回到成员分析，他的提交数、
> 增删行数、被审查任务、关联问题全部出现。
>
> 部门管理员李姐为新入职的同事批量关联提交邮箱（同事还没登录过平台），团队视图立刻完整。
>
> 未来：企微通知要 @到具体人时，同一张关联表补上 IM 账号即可，无需重建。

## 1. 产品决策（对抗式）

### D1 概念与文案：用户语言优先
- 功能名「身份关联」仅用于文档/权限命名；**页面文案全部人话**：
  - 个人设置页签：**「我的提交邮箱」**（说明：关联你用这些邮箱提交的代码，即可查看自己的审查数据）
  - 团队视图分组：**「未关联成员」**（说明：以下提交身份尚未关联到平台账号）
- 对抗质疑：叫「邮箱」会不会限制未来 IM 关联？——IM 关联在个人设置是独立区块
  （「我的 IM 账号」，预留位），两块文案互不干扰。

### D2 关联来源三种，手动为主、自动为辅
| 来源 | 场景 | 交互 |
|---|---|---|
| SELF（主路径） | 本人在「我的提交邮箱」添加 | 候选列表（带真实提交样例）**或**手动输入邮箱/名称 |
| AUTO | 系统按 sys_user.email/user_name 匹配出建议 | **仅在成员分析本人视图空态提示**，点击跳转设置页，本人确认后生效；不静默绑定 |
| ADMIN | 部门负责人指派**本部门**成员；平台管理员全量指派 | 成员身份管理视图，记操作日志 |

- 对抗质疑 1：为什么 AUTO 不做成静默自动绑定？——git author 名字重名常见（两个「张伟」），
  静默绑定会把别人的提交算到错误的人头上，企业场景属于数据事故；确认一步不可省。
- 对抗质疑 2：为什么候选要带提交样例？——企业账号名和 git 名经常对不上，用户靠
  「这笔提交是我写的」来认，不靠名字猜。候选卡展示：身份标识 + 最近一笔提交
  （仓库 + 提交消息 + 时间）。

### D3 匹配规则（只产建议，不产绑定）
- 精确：commit author_email = sys_user.email（忽略大小写）
- 名称候选：commit author_name = sys_user.user_name/nick_name → 列为「待确认候选」
- 一个提交身份只能关联一个用户（唯一约束）；一个用户可关联多个身份（多邮箱/多设备）

### D4 冲突处理（v2 新增）
- 用户添加已被他人关联的邮箱 → 提示「该邮箱已关联到用户 X（部门 Y），如归属有误请联系管理员调整」，**不报错码**；
- 管理员改派：解除原关联 → 建立新关联，两条操作日志，成员统计下次聚合生效；
- 本人删除关联：即时生效（次日聚合后数据归属变化），删除动作记日志。

### D5 IM 身份预留
identity_type 枚举 `GIT_COMMIT` / `IM_WECOM` / `IM_DINGTALK` / `IM_FEISHU`；本期只实现 GIT_COMMIT
与个人设置「我的 IM 账号」预留位（灰态 + 「即将支持」）。通知策略 @人 路由未来直接消费本表。

## 2. 数据结构

```sql
-- sql/42_identity_binding.sql（幂等存储过程风格，须 utf8mb4）
CREATE TABLE sys_user_identity (
  id            bigint AUTO_INCREMENT,
  user_id       bigint NOT NULL COMMENT '平台用户ID',
  identity_type varchar(20) NOT NULL COMMENT '身份类型(GIT_COMMIT/IM_WECOM/IM_DINGTALK/IM_FEISHU)',
  identifier    varchar(320) NOT NULL COMMENT '身份标识(GIT=提交邮箱或名称；IM=账号ID)',
  display_name  varchar(128) DEFAULT NULL COMMENT '展示名',
  origin        varchar(10) NOT NULL DEFAULT 'SELF' COMMENT '关联来源(SELF/AUTO/ADMIN)',
  create_by     varchar(64), create_time datetime,
  PRIMARY KEY (id),
  UNIQUE KEY uk_identity (identity_type, identifier),
  KEY idx_identity_user (user_id, identity_type)
);
-- 存量迁移：review_insight_identity_claim 全量搬入（identity_type=GIT_COMMIT，origin=SELF），
-- 迁移幂等（按 user_id+identifier 判重）；旧表保留不删（历史审计），代码停用
-- 菜单/权限：个人设置页签无需菜单（用户资料页内）；
-- 「成员身份管理」按钮权限 insight:identity:manage 挂成员分析菜单（135）下，
-- 授权角色：admin（role 1 默认全量）、普通角色（role 2）按需
```

## 3. 页面与交互（用户视角走查）

1. **成员分析-本人视图空态**（痛点入口）：
   「关联你的提交邮箱后，这里会展示你的提交趋势、被审查情况和关联问题。」
   [去关联] 按钮 → 跳转个人设置「我的提交邮箱」页签
2. **个人设置-「我的提交邮箱」**：
   - 已关联列表：邮箱/名称 + 来源标签（自己添加/系统建议/管理员指派）+ 移除
   - 添加区：候选卡（AUTO 建议 + 提交样例：仓库·提交消息·时间，[确认关联]）
     + 手动输入框（邮箱或 git 名称，回车添加）
   - 冲突提示按 D4 文案
3. **成员分析-团队视图**：按关联合并统计（同人多身份聚合成一行，展示名=用户昵称，
   悬浮可见其关联的身份列表）；底部「未关联成员」分组，管理员行内 [指派]
4. **成员身份管理**（insight:identity:manage）：全量关联清单（用户×身份×来源×时间）、
   指派/改派/解除；部门负责人仅见本部门；所有操作进操作日志

## 4. 接口

- GET/POST/DELETE /system/userprofile/identities（本人 GIT 身份 CRUD，登录即可）
- GET /insight/identity/candidates（本人候选：邮箱精确命中 + 名称候选 + 提交样例）
- GET /insight/team/identities、POST /insight/team/identities/bind、
  DELETE /insight/team/identities/{id}（管理端，insight:identity:manage，@Log）
- /insight/member/mine、/insight/team/members 改按关联合并统计；
  **旧接口 /insight/member/claim 与前端认领交互移除**（前端 member/index.vue 重构）

## 5. 扩展性

| 演进 | 本设计预留 |
|---|---|
| 通知 @人 路由 | IM 身份类型已在表内，通知策略切片直接消费 |
| 「我的审查记录」按发起人过滤 | PR 作者与提交身份同表关联后可复用 |
| 多企业/SSO | identifier 为字符串标识，接身份源时加 provider 列即可，不动交互 |

## 6. 验收（用户可感知口径）

1. 新用户添加提交邮箱后，本人视图 10 分钟内（下次聚合刷新）出现自己的数据；
2. 未关联时本人视图是引导文案而不是空白或「认领」黑话；
3. 同人两身份合并为一行，团队视图不再出现一人两条；
4. 两人关联同一邮箱：后者看到人话提示，管理员改派留两条日志；
5. 旧「认领」入口消失，存量 claim 数据在本人视图正常生效。
