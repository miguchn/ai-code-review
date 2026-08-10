<template>
  <div class="app-container insight-placeholder">
    <el-empty description="报告中心即将上线">
      <template #description>
        <p class="title">报告中心即将上线</p>
        <p class="desc">三期将提供周/月模板快照、异步导出与发送记录。当前请使用总览看板与项目分析查看准实时指标。</p>
      </template>
      <el-button type="primary" @click="$router.push('/insight/overview')">前往总览看板</el-button>
    </el-empty>
  </div>
</template>

<style scoped lang="scss">
.insight-placeholder {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.title {
  margin: 0 0 8px;
  font-size: 16px;
  color: var(--text-regular);
}
.desc {
  margin: 0;
  max-width: 420px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}
</style>
