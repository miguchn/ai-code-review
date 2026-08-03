<template>
  <div class="app-container workbench">
    <div v-if="loading" class="wb-loading" v-loading="true" element-loading-text="加载工作台…" />

    <template v-else-if="loadError">
      <el-empty description="工作台加载失败">
        <el-button type="primary" @click="loadSummary">重试</el-button>
      </el-empty>
    </template>

    <template v-else-if="showZeroProjectGuide">
      <div class="wb-guide">
        <el-empty description="接入第一个项目才能开始审查">
          <el-button
            v-if="canAddProject"
            type="primary"
            @click="$router.push('/review/project')"
          >接入第一个项目</el-button>
          <p v-else class="wb-guide-hint">当前账号没有项目接入权限，请联系管理员创建代码项目。</p>
        </el-empty>
      </div>
    </template>

    <template v-else>
      <section v-if="showScopeBar" class="wb-scope">
        <span v-if="summary.scope.projectCount != null" class="wb-scope-item">
          可见项目 <strong>{{ summary.scope.projectCount }}</strong>
        </span>
        <span v-if="summary.scope.latestTaskTime" class="wb-scope-item">
          最近任务
          <el-tooltip :content="summary.scope.latestTaskTime" placement="top">
            <strong class="wb-rel-time">{{ relativeTime(summary.scope.latestTaskTime) }}</strong>
          </el-tooltip>
        </span>
      </section>

      <section v-if="summary.cards?.length" class="wb-cards">
        <div
          v-for="card in summary.cards"
          :key="card.type"
          class="wb-card"
          :class="{ 'is-zero': card.count === 0 }"
          role="button"
          tabindex="0"
          @click="openCard(card)"
          @keyup.enter="openCard(card)"
        >
          <div class="wb-card-head">
            <svg-icon :icon-class="cardIcon(card.type)" class-name="wb-card-icon" />
            <span class="wb-card-title">{{ card.title }}</span>
          </div>
          <div class="wb-card-count" :class="{ 'is-zero': card.count === 0 }">{{ card.count }}</div>
        </div>
      </section>

      <section class="wb-today">
        <h3 class="wb-section-title">今日摘要</h3>
        <div class="wb-today-grid">
          <div class="wb-today-item">
            <div class="wb-today-label">新增任务</div>
            <div class="wb-today-value">{{ displayToday(summary.today?.newTasks) }}</div>
          </div>
          <div class="wb-today-item">
            <div class="wb-today-label">成功任务</div>
            <div class="wb-today-value">{{ displayToday(summary.today?.successTasks) }}</div>
          </div>
          <div class="wb-today-item">
            <div class="wb-today-label">失败任务</div>
            <div class="wb-today-value">{{ displayToday(summary.today?.failedTasks) }}</div>
          </div>
          <div class="wb-today-item">
            <div class="wb-today-label">关闭问题</div>
            <div class="wb-today-value">{{ displayToday(summary.today?.closedIssues) }}</div>
          </div>
        </div>
      </section>

      <section class="wb-recent">
        <div class="wb-recent-head">
          <h3 class="wb-section-title">最近动态</h3>
          <el-button
            v-if="canViewTasks"
            link
            type="primary"
            @click="$router.push('/review/task')"
          >查看全部</el-button>
        </div>
        <el-empty v-if="!summary.recent?.length" description="暂无审查动态" :image-size="64" />
        <ul v-else class="wb-recent-list">
          <li
            v-for="(item, idx) in summary.recent"
            :key="idx"
            class="wb-recent-item"
            @click="openRecent(item)"
          >
            <span class="wb-recent-title">{{ item.title }}</span>
            <el-tooltip v-if="item.time" :content="item.time" placement="top">
              <span class="wb-rel-time">{{ relativeTime(item.time) }}</span>
            </el-tooltip>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>

<script setup name="Index">
import { getWorkbenchSummary } from '@/api/workbench'
import auth from '@/plugins/auth'
import { formatTime } from '@/utils/index'

const CARD_ICON = {
  ISSUE_AWAITING_CONFIRM: 'bug',
  ISSUE_AWAITING_FIX: 'edit',
  HIGH_RISK_CONCLUSION: 'validCode',
  TASK_FAILED: 'job',
  DELIVERY_FAILED: 'message'
}

const router = useRouter()
const { proxy } = getCurrentInstance()

const loading = ref(true)
const loadError = ref(false)
const summary = ref({
  scope: {},
  cards: [],
  today: {},
  recent: []
})

const canAddProject = computed(() => auth.hasPermi('review:project:add'))
const canViewTasks = computed(() => auth.hasPermi('review:task:list'))

const showZeroProjectGuide = computed(() => summary.value?.scope?.projectCount === 0)

const showScopeBar = computed(() => {
  const scope = summary.value?.scope || {}
  return scope.projectCount != null || scope.latestTaskTime
})

function cardIcon(type) {
  return CARD_ICON[type] || 'dashboard'
}

function displayToday(value) {
  return value == null ? '—' : value
}

function relativeTime(timeStr) {
  if (!timeStr) return '—'
  const ms = new Date(timeStr.replace(/-/g, '/')).getTime()
  if (Number.isNaN(ms)) return timeStr
  return formatTime(ms)
}

function openCard(card) {
  if (!card?.link) return
  router.push({ path: card.link, query: card.query || {} })
}

function openRecent(item) {
  if (!item?.link) return
  router.push(item.link)
}

function loadSummary() {
  loading.value = true
  loadError.value = false
  getWorkbenchSummary().then(res => {
    summary.value = res.data || { scope: {}, cards: [], today: {}, recent: [] }
  }).catch(() => {
    loadError.value = true
    proxy.$modal.msgError('工作台加载失败，请重试')
  }).finally(() => {
    loading.value = false
  })
}

onMounted(() => loadSummary())
onActivated(() => loadSummary())
</script>

<style scoped lang="scss">
.workbench {
  padding-bottom: 32px;
}

.wb-loading {
  min-height: 240px;
}

.wb-guide {
  padding: 48px 16px;
}

.wb-guide-hint {
  margin: 12px 0 0;
  color: var(--text-secondary, #64748b);
  font-size: 13px;
  line-height: 20px;
}

.wb-scope {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 28px;
  margin-bottom: 20px;
  padding: 14px 18px;
  background: var(--brand-bg-soft, #eaf7ee);
  border: 1px solid var(--brand-border, #bce7c8);
  border-radius: 8px;
  color: var(--text-regular, #334155);
  font-size: 14px;
  line-height: 22px;
}

.wb-scope-item strong {
  margin-left: 4px;
  color: var(--text-primary, #0f172a);
  font-weight: 600;
}

.wb-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}

.wb-card {
  padding: 16px;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter, #e2e8f0);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.wb-card:hover,
.wb-card:focus-visible {
  border-color: var(--brand-border, #bce7c8);
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.06);
  outline: none;
}

.wb-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: var(--text-regular, #334155);
  font-size: 13px;
  line-height: 20px;
}

.wb-card-icon {
  color: var(--brand-icon, #16843f);
  font-size: 16px;
}

.wb-card.is-zero .wb-card-icon {
  color: var(--text-secondary, #94a3b8);
}

.wb-card-count {
  font-size: 28px;
  line-height: 36px;
  font-weight: 600;
  color: var(--text-primary, #0f172a);
  font-variant-numeric: tabular-nums;
}

.wb-card-count.is-zero {
  color: var(--text-secondary, #94a3b8);
}

.wb-section-title {
  margin: 0 0 12px;
  color: var(--text-primary, #0f172a);
  font-size: 16px;
  line-height: 24px;
  font-weight: 600;
}

.wb-today {
  margin-bottom: 24px;
}

.wb-today-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

@media (max-width: 900px) {
  .wb-today-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.wb-today-item {
  padding: 14px 16px;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter, #e2e8f0);
  border-radius: 8px;
}

.wb-today-label {
  margin-bottom: 6px;
  color: var(--text-secondary, #64748b);
  font-size: 13px;
  line-height: 20px;
}

.wb-today-value {
  color: var(--text-primary, #0f172a);
  font-size: 22px;
  line-height: 28px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.wb-recent-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
}

.wb-recent-head .wb-section-title {
  margin-bottom: 0;
}

.wb-recent-list {
  margin: 0;
  padding: 0;
  list-style: none;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter, #e2e8f0);
  border-radius: 8px;
  overflow: hidden;
}

.wb-recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-extra-light, #f1f5f9);
  cursor: pointer;
}

.wb-recent-item:last-child {
  border-bottom: none;
}

.wb-recent-item:hover {
  background: var(--brand-bg-soft, #eaf7ee);
}

.wb-recent-title {
  min-width: 0;
  color: var(--text-regular, #334155);
  font-size: 14px;
  line-height: 22px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wb-rel-time {
  flex-shrink: 0;
  color: var(--text-secondary, #64748b);
  font-size: 12px;
  line-height: 18px;
  cursor: default;
}
</style>
