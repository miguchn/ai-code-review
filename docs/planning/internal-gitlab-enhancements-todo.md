# 内网 GitLab 落地增强待办

> **实现状态：已完成**（2026-09-09，commit `5b7839c`/`e4ee47e` 合入 main）
> 第 8 条身份映射已落地（复用 sys_user_identity + GIT_PLATFORM_USER + 自动发现 + 管理员绑定），
> 第 9 条入站诊断文案已落地。以下为设计记录。

> 2026-09-09 长城官微 GitLab 联调验证中发现的两个内网专属场景问题，
> 需独立于 UX 优化批单独推进。来源：联调复盘第 8、9 条。

## 8. 身份绑定覆盖率——内网 GitLab 自动指派失效

**现象**：M13 问题自动指派靠 email/username 匹配 sys_user。内网 GitLab 的 commit author
（如「李超 / li.chao / xiaofengzi_810@163.com」）与 sys_user 账号体系大概率对不上，
导致问题台账无人指派、逾期提醒无人收——自动指派闭环在内网场景名存实亡。

**影响**：问题生命周期闭环（指派→救济→逾期聚合提醒）在内网 GitLab 场景失效。

**建议方案**：
- 支持「GitLab 用户名/邮箱 → sys_user」的显式映射表（项目级或平台级配置）
- 或对接公司 ID/HR 系统，按工号统一身份
- 兜底：未匹配时按项目默认负责人（owner_user_id）指派，而非悬空

## 9. 凭据/Secret 配置无内网可达性预检

**现象**：凭据连接测试能验「ACR → GitLab」出站通（今天实测 SUCCESS），
但验不了「GitLab → ACR」入站通。今天配置全绿却实际入站死路（execution expired），
这种"单向绿"的假阳性是联调最大时间陷阱。

**影响**：用户配置完一切看着全绿，实际 webhook 永远到不了——排障全靠日志/抓包破案。

**建议方案**：
- 项目 webhook 配置页加「入站连通自检」：ACR 主动向 GitLab 注册一个一次性 test hook
  并等待回调，或引导用户在 GitLab 点 Test 后回看 `last_webhook_time` 是否更新
- 凭据测试通过后，在项目详情页展示「入站状态：未知/已验证/超时」，
  而非只展示出站连接成功
- 回调地址探测到的内网 IP（已由 feature/review-ux-polish 实现）在此处复用展示

## 关联

- UX 优化批（7 项）：`feature/review-ux-polish` 分支
- 联调实录：memory `project_acr_local_env.md` 2026-09-09 条目
- 根因 bug 已修：GitLab compare diff 丢元数据（main 已合 `b7147eb`）
