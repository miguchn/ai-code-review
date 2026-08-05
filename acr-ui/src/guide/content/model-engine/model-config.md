# 模型服务配置

这篇帮你在平台新增一个大模型配置、完成连通检测，并理解「默认模型」的作用。

## 前提

平台通过 OpenAI Compatible 协议调用模型服务（Claude 使用 Anthropic 原生协议，平台自动适配）。模型服务可以是公有云厂商，也可以是内网自建的兼容服务。

## 新增模型配置

进入「策略配置 → 大模型配置」，点击「新增」，字段对照如下：

| 字段 | 说明 |
|---|---|
| 配置名称 | 任意名称，如 `DeepSeek-生产` |
| 服务厂商 | DeepSeek / Kimi / 通义千问 / 百炼 / 豆包 / OpenAI / Claude / 其他/自定义；选择后会自动填充推荐的 API 地址与模型标识，可再修改 |
| 厂商名称 | 仅服务厂商选「其他/自定义」时出现，填自定义厂商的显示名称 |
| API 地址 | Chat Completions 接口完整地址（Claude 填原生基址） |
| 模型标识 | 厂商文档中的模型名，如 `deepseek-chat` |
| API Key | 厂商控制台创建；加密存储，保存后不回显明文；编辑时留空表示不修改，**修改服务厂商或 API 地址时必须重新填写** |
| 是否启用 | 仅启用状态的配置可被项目选择 |
| 是否默认 | 见下文「默认模型的作用」 |
| 超时(ms) / 最大 Token / Temperature / 上下文长度 | 高级参数，默认值 60000 / 8000 / 0.7 / 128000，可按模型能力调整 |
| 排序 / 备注 | 可选 |

## 各厂商 API Key 获取与地址参考

| 服务厂商 | API Key 获取入口（厂商控制台） | API 地址（自动填充，可改） | 模型标识示例 |
|---|---|---|---|
| DeepSeek | DeepSeek 开放平台 → API Keys | `https://api.deepseek.com/v1/chat/completions` | `deepseek-chat` |
| Kimi | Kimi 开放平台 → API Key 管理 | `https://api.moonshot.cn/v1/chat/completions` | `moonshot-v1-8k` |
| 通义千问 / 百炼 | 阿里云百炼控制台 → API-KEY 管理 | `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions` | `qwen-plus` |
| 豆包 | 火山方舟控制台 → API Key 管理 | `https://ark.cn-beijing.volces.com/api/v3/chat/completions` | 填方舟推理接入点 ID（形如 `ep-xxxx…`） |
| OpenAI | OpenAI 平台 → API keys | `https://api.openai.com/v1/chat/completions` | `gpt-4o-mini` |
| Claude | Anthropic 控制台 → API Keys | `https://api.anthropic.com` | `claude-3-5-sonnet-latest` |
| 其他/自定义 | 自建服务（如 vLLM）自行签发 | 填完整 Chat Completions 地址 | 与自建服务模型名一致 |

> 表中为外部厂商地址示例。内网部署且无外网出口时，请选用内网可达的模型服务或自建服务（选「其他/自定义」）。API Key 一律以你在厂商控制台实际创建的为准，形如 `sk-xxxx…` 仅为占位示意。

## 检测（验证配置是否可用）

在新增/编辑弹窗中有两个检测按钮：

- **连接测试**：以最小参数发起一次试探调用，验证 API 地址可达、API Key 有效；
- **模型调用测试**：以接近审查的参数发起调用，验证模型标识与参数组合可用。

检测结果会写回模型列表的「最近检测结果」列：成功显示「成功 (耗时 ms)」；失败显示错误类型与原因，如「认证失败: xxx」。检测记录同时展示在「最近检测时间」列。各错误类型的排查见「模型与引擎 → 模型检测失败排查」。

## 默认模型的作用

列表操作中的「设为默认」（或编辑时勾选「是否默认」）：

1. **审查引擎方式的运行时模型**：项目选择「审查引擎」方式时，引擎运行使用平台默认模型；没有已启用的默认模型时，引擎类任务会失败并提示「请先在模型服务配置并启用默认模型」；
2. **审查引擎页「测试调用」的默认选中项**；
3. 模型列表与工作台展示「默认」标记。

约束：平台同时只保留一个默认模型（设新默认会自动取消旧默认）；默认模型必须处于启用状态，不能直接禁用或取消默认，需先切换默认模型。

> 注意：新建项目时模型仍需手动选择，「默认」不会自动带入项目表单。
