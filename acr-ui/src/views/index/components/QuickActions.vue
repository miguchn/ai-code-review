<template>
  <section v-if="actions.length" class="wb-panel">
    <header class="wb-panel-head">
      <h3 class="wb-panel-title">快捷操作</h3>
    </header>
    <ul class="wb-quick-list">
      <li
        v-for="action in actions"
        :key="action.path"
        class="wb-quick-item"
        role="button"
        tabindex="0"
        @click="go(action)"
        @keyup.enter="go(action)"
      >
        <svg-icon :icon-class="action.icon" class-name="wb-quick-icon" />
        <div class="wb-quick-text">
          <div class="wb-quick-title">{{ action.title }}</div>
          <div class="wb-quick-desc">{{ action.desc }}</div>
        </div>
        <el-icon class="wb-quick-arrow"><ArrowRight /></el-icon>
      </li>
    </ul>
  </section>
</template>

<script setup>
import { ArrowRight } from '@element-plus/icons-vue'
import auth from '@/plugins/auth'

const router = useRouter()

const ACTIONS = [
  { title: '接入项目', desc: '新增代码仓库与审查范围', icon: 'code', path: '/project-access/project', perm: 'review:project:add' },
  { title: '问题台账', desc: '确认、修复与关闭问题', icon: 'bug', path: '/review/issue', perm: 'review:issue:list' },
  { title: '审查任务', desc: '执行队列与失败重试', icon: 'job', path: '/review/task', perm: 'review:task:list' },
  { title: '审查记录', desc: '已完成审查的结论与评分', icon: 'form', path: '/review/record', perm: 'review:record:list' },
  { title: '投递记录', desc: '评论回写与通知投递状态', icon: 'message', path: '/notify/delivery', perm: 'review:delivery:list' },
  { title: '模型服务', desc: '模型连接与可用性管理', icon: 'ai', path: '/model-service/ai-model-config', perm: 'system:aimodelconfig:list' }
]

const actions = computed(() => ACTIONS.filter(a => auth.hasPermi(a.perm)))

function go(action) {
  router.push(action.path)
}
</script>

<style scoped lang="scss">
.wb-quick-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.wb-quick-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-control, 8px);
  cursor: pointer;
  transition: background 0.15s ease;

  &:hover,
  &:focus-visible {
    background: var(--bg-hover);
    outline: none;
  }

  & + & {
    margin-top: 2px;
  }
}

.wb-quick-icon {
  flex-shrink: 0;
  font-size: 17px;
  color: var(--brand-icon, var(--brand-600));
}

.wb-quick-text {
  min-width: 0;
  flex: 1;
}

.wb-quick-title {
  color: var(--text-regular);
  font-size: 14px;
  line-height: 20px;
  font-weight: 500;
}

.wb-quick-desc {
  margin-top: 1px;
  color: var(--text-assist);
  font-size: 12px;
  line-height: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wb-quick-arrow {
  flex-shrink: 0;
  color: var(--text-placeholder);
  font-size: 14px;

  .wb-quick-item:hover & {
    color: var(--brand-600);
  }
}
</style>
