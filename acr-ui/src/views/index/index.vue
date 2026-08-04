<template>
  <div class="app-container workbench">
    <div v-if="initialLoading" class="wb-loading" v-loading="true" element-loading-text="加载工作台…" />

    <template v-else-if="showZeroProjectGuide">
      <div class="wb-guide">
        <el-empty description="接入第一个项目才能开始审查">
          <el-button
            v-if="canAddProject"
            type="primary"
            @click="$router.push('/project-access/project')"
          >接入第一个项目</el-button>
          <p v-else class="wb-guide-hint">当前账号没有项目接入权限，请联系管理员创建代码项目。</p>
        </el-empty>
      </div>
    </template>

    <template v-else>
      <header class="wb-header">
        <h2 class="wb-title">工作台</h2>
        <div v-if="showScopeInfo" class="wb-scope">
          <span v-if="scope.projectCount != null" class="wb-scope-item">
            可见项目 <strong>{{ scope.projectCount }}</strong>
          </span>
          <span v-if="scope.latestTaskTime" class="wb-scope-item">
            最近任务
            <el-tooltip :content="scope.latestTaskTime" placement="top">
              <strong class="wb-rel-time">{{ relativeTime(scope.latestTaskTime) }}</strong>
            </el-tooltip>
          </span>
        </div>
      </header>

      <div class="wb-columns">
        <div class="wb-col-main">
          <TodoCards
            :cards="cards"
            :loading="summaryReq.loading.value"
            :error="summaryReq.error.value"
            @open="openCard"
            @retry="loadSummary"
          />
          <TrendChart
            v-if="trendAvailable"
            :trend="trendReq.data.value"
            :loading="trendReq.loading.value"
            :error="trendReq.error.value"
            @retry="loadTrend"
          />
          <RecentActivity
            v-if="canViewTasks"
            :recent="recent"
            :error="summaryReq.error.value"
            @open="openRecent"
            @retry="loadSummary"
            @view-all="$router.push('/review/task')"
          />
        </div>
        <div class="wb-col-side">
          <QuickActions />
          <TodaySummary
            :today="today"
            :error="summaryReq.error.value"
            @retry="loadSummary"
          />
          <ModelHealth
            :models="modelsReq.data.value"
            :loading="modelsReq.loading.value"
            :error="modelsReq.error.value"
            :can-manage="canManageModels"
            @retry="loadModels"
            @go-models="$router.push('/model-service/ai-model-config')"
          />
          <ConclusionOverview
            v-if="trendAvailable"
            :trend="trendReq.data.value"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup name="Index">
import { getWorkbenchSummary, getWorkbenchTrend, getWorkbenchModels } from '@/api/workbench'
import auth from '@/plugins/auth'
import TodoCards from './components/TodoCards.vue'
import QuickActions from './components/QuickActions.vue'
import TrendChart from './components/TrendChart.vue'
import TodaySummary from './components/TodaySummary.vue'
import ConclusionOverview from './components/ConclusionOverview.vue'
import ModelHealth from './components/ModelHealth.vue'
import RecentActivity from './components/RecentActivity.vue'
import { relativeTime } from './utils'

const router = useRouter()

/** 单请求状态封装：失败不弹全局消息，交区块内重试 */
function useRequest(fetcher) {
  const data = ref(null)
  const error = ref(false)
  const loading = ref(true)
  function load() {
    loading.value = true
    error.value = false
    return fetcher().then(res => {
      data.value = res.data ?? null
    }).catch(() => {
      error.value = true
    }).finally(() => {
      loading.value = false
    })
  }
  return { data, error, loading, load }
}

const summaryReq = useRequest(getWorkbenchSummary)
const trendReq = useRequest(() => getWorkbenchTrend(14))
const modelsReq = useRequest(getWorkbenchModels)

const canAddProject = computed(() => auth.hasPermi('review:project:add'))
const canViewTasks = computed(() => auth.hasPermi('review:task:list'))
const canManageModels = computed(() => auth.hasPermi('system:aimodelconfig:list'))

const scope = computed(() => summaryReq.data.value?.scope || {})
const cards = computed(() => summaryReq.data.value?.cards || [])
const today = computed(() => summaryReq.data.value?.today || {})
const recent = computed(() => summaryReq.data.value?.recent || [])

const initialLoading = computed(() => summaryReq.loading.value && !summaryReq.data.value && !summaryReq.error.value)
const showZeroProjectGuide = computed(() => summaryReq.data.value?.scope?.projectCount === 0)
const showScopeInfo = computed(() => scope.value.projectCount != null || !!scope.value.latestTaskTime)
/** 无 review:record:list 权限时 trend 为 null，趋势与结果概览区块整体隐藏 */
const trendAvailable = computed(() => trendReq.data.value != null)

function loadSummary() { return summaryReq.load() }
function loadTrend() { return trendReq.load() }
function loadModels() { return modelsReq.load() }

let lastLoadAt = 0

function loadAll() {
  lastLoadAt = Date.now()
  loadSummary()
  loadTrend()
  loadModels()
}

function openCard(card) {
  if (!card?.link) return
  router.push({ path: card.link, query: card.query || {} })
}

function openRecent(item) {
  if (!item?.link) return
  router.push(item.link)
}

// 首次进入：TagsView 写入 cachedViews 可能晚于页面首次渲染，未被 keep-alive 缓存的组件
// 不触发 onActivated，首次加载必须在 onMounted 完成；重入 tab 的刷新依赖 onActivated
onMounted(() => loadAll())
onActivated(() => {
  // 挂载同拍（mounted 后紧随）的激活与 onMounted 重复，按时间窗跳过；
  // 不能用次数守卫——首次挂载未缓存时没有 activated 可消耗守卫，会错误跳过第一次重入
  if (Date.now() - lastLoadAt < 800) return
  loadAll()
})
</script>

<style lang="scss">
/* 工作台共享面板样式（嵌套 .workbench 作用域，不外溢） */
.workbench {
  padding-bottom: 32px;

  .wb-loading {
    min-height: 240px;
  }

  .wb-guide {
    padding: 48px 16px;
  }

  .wb-guide-hint {
    margin: 12px 0 0;
    color: var(--text-secondary);
    font-size: 13px;
    line-height: 20px;
  }

  .wb-header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 8px 24px;
    margin-bottom: 16px;
  }

  .wb-title {
    margin: 0;
    color: var(--text-primary);
    font-size: 20px;
    line-height: 28px;
    font-weight: 600;
    letter-spacing: -0.01em;
  }

  .wb-scope {
    display: flex;
    flex-wrap: wrap;
    gap: 6px 24px;
    color: var(--text-secondary);
    font-size: 13px;
    line-height: 22px;
  }

  .wb-scope-item strong {
    margin-left: 4px;
    color: var(--text-primary);
    font-weight: 600;
  }

  .wb-columns {
    display: grid;
    grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
    gap: 16px;
    align-items: start;
  }

  @media (max-width: 1199px) {
    .wb-columns {
      grid-template-columns: minmax(0, 1fr);
    }
  }

  .wb-col-main,
  .wb-col-side {
    display: flex;
    flex-direction: column;
    gap: 16px;
    min-width: 0;
  }

  .wb-panel {
    padding: 18px 20px;
    background: var(--neutral-card);
    border: 1px solid var(--border-light);
    border-radius: var(--radius-card, 12px);
  }

  .wb-panel-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 14px;
  }

  .wb-panel-title {
    margin: 0;
    color: var(--text-primary);
    font-size: 15px;
    line-height: 24px;
    font-weight: 600;
  }

  .wb-panel-extra {
    color: var(--text-assist);
    font-size: 12px;
    line-height: 18px;
  }

  .wb-block-state {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    min-height: 72px;
    color: var(--text-secondary);
    font-size: 13px;
  }
}
</style>
