<template>
  <div class="app-container home">
    <!-- 欢迎区域 -->
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-inner">
        <h1>欢迎使用 AI Code Review</h1>
        <p>基于 AI 与规则引擎的智能代码审查平台</p>
        <p class="welcome-sub">审查推送 · 国产大模型 · 可视化后台 · 历史追溯</p>
      </div>
    </el-card>

    <!-- 平台能力概览（占位，后续按真实模块填充） -->
    <el-row :gutter="20" class="feature-row">
      <el-col :xs="24" :sm="24" :md="8" v-for="cap in capabilities" :key="cap.title">
        <el-card shadow="never" class="feature-card">
          <template #header>
            <div class="feature-header">
              <svg-icon :icon-class="cap.icon" class-name="home-feature-icon" />
              <span>{{ cap.title }}</span>
            </div>
          </template>
          <div class="feature-body">
            <p>{{ cap.desc }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="placeholder-card">
      <el-empty description="业务模块开发中，敬请期待" />
    </el-card>
  </div>
</template>

<script setup name="Index">
const capabilities = [
  { title: '审查引擎', icon: 'code', desc: '集成 alibaba/open-code-review，支持 diff 增量审查与全量扫描' },
  { title: '通知推送', icon: 'message', desc: '审查结果推送钉钉 / 企微 / 飞书群，主动触达开发者' },
  { title: '管理后台', icon: 'system', desc: '模型配置、项目与权限、审查规则、历史记录可视化管理' }
]
</script>

<style scoped lang="scss">
.home {
  padding-bottom: 32px;
}

.welcome-card {
  margin-bottom: 20px;
  background: var(--brand-bg-soft) !important;
  border-color: var(--brand-border) !important;
  :deep(.el-card__body) {
    padding: 24px 28px !important;
  }
}
.welcome-inner {
  color: var(--text-regular);
  h1 {
    margin: 0 0 8px;
    color: var(--text-primary);
    font-size: 24px;
    line-height: 32px;
    font-weight: 600;
  }
  p {
    margin: 0;
    color: var(--text-regular);
    font-size: 14px;
    line-height: 22px;
  }
  .welcome-sub {
    margin-top: 4px;
    color: var(--text-secondary);
    font-size: 13px;
  }
}

.feature-row {
  margin-bottom: 20px;
}
.feature-card {
  height: 100%;
  :deep(.el-card__header) {
    padding: 16px 20px !important;
    border-bottom-color: var(--divider);
  }
}
.feature-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  :deep(.svg-icon) {
    font-size: 18px;
    width: 1em;
    height: 1em;
    flex-shrink: 0;
    color: var(--brand-500);
  }
}
.feature-body {
  padding: 0 4px 8px;
  p {
    margin: 4px 0;
    font-size: 13px;
    color: var(--text-secondary);
    line-height: 20px;
  }
}

.placeholder-card {
  :deep(.el-card__body) {
    padding: 32px 0 !important;
  }
}

@media (max-width: 991px) {
  .feature-row {
    margin-bottom: 4px;
  }

  .feature-card {
    height: auto;
    margin-bottom: 16px;
  }
}

@media (max-width: 767px) {
  .welcome-card :deep(.el-card__body) {
    padding: 20px !important;
  }
}
</style>
