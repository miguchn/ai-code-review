# acr-review

`acr-review` 是代码审核主业务的统一承载模块，用于项目与 Git 接入，并在后续纵向切片中承载 Webhook 事件、审查任务与问题、策略快照、引擎编排、平台回写、通知、报告及运行保障。

P0/M1 当前已实现 GitHub 项目与 Personal Access Token 凭据管理：

- `domain`：代码项目、Git 凭据、仓库读取结果和项目表单选项；
- `mapper` / `resources/mapper/review`：项目与凭据持久化；
- `service`：项目/凭据用例、业务系统/部门/负责人校验、引用删除保护和连接检测；
- `git`：最小 `GitProvider` 契约和唯一的 GitHub 实现，支持仓库元数据及分页分支读取；
- `security`：使用环境主密钥的 AES-256-GCM 凭据加解密。

项目可从 GitHub 自动同步 owner、repository、默认分支和全部分支，并从真实分支中选择 PR 目标分支；来源分支默认全部允许。本模块未实现 Webhook、克隆、Diff、审查任务、评论回写或其他 Git 平台实现。REST Controller 仍位于 `acr-admin`，前端位于 `acr-ui`。

依赖方向：

```text
acr-admin -> acr-review -> acr-system -> acr-common
```

- `acr-admin` 只保留启动、配置以及 REST/Webhook 接入；
- `acr-review` 承载代码审核业务规则、数据访问、流程编排和外部适配；
- `acr-system` 提供用户、权限、部门、业务系统和模型配置等平台治理能力；
- `acr-common` 只保留稳定的跨模块通用能力。

运行凭据相关功能前必须由部署环境提供 `ACR_CREDENTIAL_MASTER_KEY`，值为 Base64 编码的 32 字节密钥。仓库不提供默认密钥，密钥必须在实例间和重启后保持一致。
