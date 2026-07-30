# ACR UI — AI Code Review 前端

基于 Vue 3 + Element Plus + Vite 构建的智能代码审查平台前端，提供 Web 管理后台、数据看板、通知配置等企业级能力。

## 技术栈

| 层级 | 技术 |
|------|------|
| 框架 | Vue 3 + Vue Router 4 + Pinia |
| UI | Element Plus |
| 构建 | Vite |
| 图表 | ECharts |
| HTTP | Axios |
| 语言 | JavaScript |

## 前端运行

```bash
# 进入前端目录
cd acr-ui

# 安装依赖
npm install --registry=https://registry.npmmirror.com

# 启动开发服务
npm run dev

# 构建 UAT 环境
npm run build:uat

# 构建生产环境
npm run build:prod

# 前端访问地址 http://localhost:80
```

## 目录结构

```
acr-ui/
├── src/
│   ├── api/          # 后端接口封装
│   ├── assets/       # 静态资源（图片、样式、SVG 图标）
│   ├── components/   # 通用组件
│   ├── directive/    # 自定义指令
│   ├── layout/       # 布局
│   ├── plugins/      # 全局插件（modal、tab、cache、download、auth）
│   ├── router/       # 路由
│   ├── store/        # Pinia 状态管理
│   ├── utils/        # 工具函数
│   └── views/        # 页面
└── vite/             # Vite 插件配置
```

## 后端配合

前端默认对接后端 `http://localhost:8080`（见 `vite.config.js`），启动前请先运行后端服务（`acr-admin`）。
