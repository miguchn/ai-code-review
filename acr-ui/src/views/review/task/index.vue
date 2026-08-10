<template>
  <div class="app-container">
    <el-alert
      class="mb12"
      type="info"
      :closable="false"
      show-icon
      title="审查任务是执行队列：重点关注待执行、执行中与失败重试。已完成结果请到「审查记录」查看。"
    />

    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
      <el-form-item label="所属项目" prop="projectId">
        <el-select v-model="queryParams.projectId" clearable filterable placeholder="请选择项目" style="width: 220px">
          <el-option v-for="item in projectOptions" :key="item.projectId" :label="item.projectName" :value="item.projectId" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型" prop="eventSource">
        <el-select v-model="queryParams.eventSource" clearable placeholder="请选择类型" style="width: 130px">
          <el-option v-for="dict in review_event_source" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="任务状态" prop="taskStatus">
        <el-select v-model="queryParams.taskStatus" clearable placeholder="请选择状态" style="width: 130px" @change="onStatusChange">
          <el-option v-for="dict in review_task_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="显示范围">
        <el-radio-group v-model="queryParams.queueOnly" @change="handleQuery">
          <el-radio-button :value="true">执行队列</el-radio-button>
          <el-radio-button :value="false">全部任务</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <template v-if="showAdvancedSearch">
        <el-form-item label="合并请求编号" prop="prNumber">
          <el-input v-model="queryParams.prNumber" placeholder="请输入合并请求编号" clearable style="width: 140px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="-" start-placeholder="开始日期"
            end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
      </template>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button link type="primary" @click="showAdvancedSearch = !showAdvancedSearch">
          {{ showAdvancedSearch ? '收起筛选' : '更多筛选' }}
        </el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="taskList" empty-text="暂无审查任务">
      <el-table-column label="所属项目" prop="projectName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="类型" width="100">
        <template #default="scope">
          <dict-tag :options="review_event_source" :value="scope.row.eventSource || 'PR'" />
        </template>
      </el-table-column>
      <el-table-column label="变更来源" min-width="200">
        <template #default="scope">
          <div v-if="isPushTask(scope.row)" class="pr-cell">
            <el-tag size="small" type="success">{{ formatPushRefDisplay(scope.row) }}</el-tag>
            <span class="pr-title" :title="scope.row.prTitle || ''">{{ emptyDash(scope.row.prTitle) }}</span>
          </div>
          <div v-else class="pr-cell">
            <el-tag size="small" type="primary">#{{ scope.row.prNumber }}</el-tag>
            <span class="pr-title" :title="scope.row.prTitle || ''">{{ emptyDash(scope.row.prTitle) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="分支" min-width="180">
        <template #default="scope">
          <span class="branch-flow">{{ emptyDash(scope.row.sourceBranch) }} → {{ emptyDash(scope.row.targetBranch) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="任务状态" width="100">
        <template #default="scope">
          <dict-tag :options="review_task_status" :value="scope.row.taskStatus" />
        </template>
      </el-table-column>
      <el-table-column label="当前步骤" width="120">
        <template #default="scope">
          <dict-tag v-if="scope.row.currentStep" :options="review_task_step" :value="scope.row.currentStep" />
          <span v-else class="empty-tip">—</span>
        </template>
      </el-table-column>
      <el-table-column label="执行次数" prop="attemptCount" width="90" align="center">
        <template #default="scope">{{ scope.row.attemptCount == null ? '—' : scope.row.attemptCount }}</template>
      </el-table-column>
      <el-table-column label="失败原因" min-width="160">
        <template #default="scope">
          <span v-if="scope.row.taskStatus === 'FAILED'" class="failure-message" :title="readableFailureMessage(scope.row.failureMessage)">
            {{ readableFailureMessage(scope.row.failureMessage) }}
          </span>
          <span v-else class="empty-tip">—</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="scope">{{ formatDateTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="scope">
          <el-tooltip :content="detailActionTip()" placement="top" :disabled="canOpenDetail()">
            <span class="action-wrap">
              <el-button link type="primary" :disabled="!canOpenDetail()" @click="handleDetailClick(scope.row)">详情</el-button>
            </span>
          </el-tooltip>
          <el-tooltip :content="retryActionTip(scope.row)" placement="top" :disabled="canRetry(scope.row)">
            <span class="action-wrap">
              <el-button link type="primary" :disabled="!canRetry(scope.row)" @click="handleRetryClick(scope.row)">
                {{ retryActionLabel(scope.row) }}
              </el-button>
            </span>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="ReviewTask">
import { listReviewTask, retryReviewTask } from '@/api/review/task'
import { listReviewProject } from '@/api/review/project'
import auth from '@/plugins/auth'
import { emptyDash, formatDateTime, isPushTask, formatPushRefDisplay, readableFailureMessage } from '@/utils/reviewDisplay'

const route = useRoute()
const { proxy } = getCurrentInstance()
const { review_task_status, review_task_step, review_event_source } = proxy.useDict(
  'review_task_status', 'review_task_step', 'review_event_source'
)

const taskList = ref([])
const projectOptions = ref([])
const loading = ref(true)
const showSearch = ref(true)
const showAdvancedSearch = ref(false)
const total = ref(0)
const dateRange = ref([])
const pollTimer = ref(null)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectId: undefined,
    prNumber: undefined,
    eventSource: undefined,
    taskStatus: undefined,
    queueOnly: true
  }
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  const params = proxy.addDateRange({ ...queryParams.value }, dateRange.value)
  listReviewTask(params).then(response => {
    taskList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => { loading.value = false; schedulePolling() })
}

function schedulePolling() {
  clearPolling()
  const hasRunning = taskList.value.some(row => row.taskStatus === 'RUNNING')
  if (hasRunning) {
    pollTimer.value = setTimeout(() => { getList() }, 5000)
  }
}

function clearPolling() {
  if (pollTimer.value) { clearTimeout(pollTimer.value); pollTimer.value = null }
}

onBeforeUnmount(() => clearPolling())

function loadProjects() {
  listReviewProject({ pageNum: 1, pageSize: 100 }).then(response => {
    projectOptions.value = response.rows || []
  })
}

function canOpenDetail() {
  return auth.hasPermi('review:task:query')
}

function canRetry(row) {
  return auth.hasPermi('review:task:retry')
    && (row?.taskStatus === 'FAILED' || row?.taskStatus === 'PENDING' || row?.taskStatus === 'RUNNING')
}

function retryActionLabel(row) {
  return row?.taskStatus === 'PENDING' ? '执行' : '重试'
}

function statusLabel(status) {
  const hit = (review_task_status.value || []).find(item => item.value === status)
  return hit ? hit.label : status
}

function detailActionTip() {
  if (!auth.hasPermi('review:task:query')) {
    return '当前账号没有「审查任务详情」权限（review:task:query）。请联系管理员授权后重新登录。'
  }
  return '查看任务执行概览与排障信息'
}

function retryActionTip(row) {
  if (!auth.hasPermi('review:task:retry')) {
    return '当前账号没有「审查任务执行/重试」权限（review:task:retry）。请联系管理员授权后重新登录。'
  }
  const status = row?.taskStatus
  if (status === 'FAILED') return '任务已失败，可重试；历史执行记录会保留。'
  if (status === 'PENDING') return '任务尚未开始。若长时间仍为待执行，可点「执行」手动触发。'
  if (status === 'RUNNING') {
    return `当前为「${statusLabel(status)}」。请等待结束；若已中断超过 30 分钟，可点「重试」回收。`
  }
  if (status === 'SUCCESS') return '已完成任务不可再次执行。结果请到「审查记录」查看。'
  return `仅「待执行」或「已失败」任务可手动触发。当前状态：${statusLabel(status) || '未知'}。`
}

function handleDetailClick(row) {
  if (!canOpenDetail()) {
    proxy.$modal.msgWarning(detailActionTip())
    return
  }
  proxy.$router.push('/review/task-detail/index/' + row.taskId)
}

function handleRetryClick(row) {
  if (!canRetry(row)) {
    proxy.$modal.msgWarning(retryActionTip(row))
    return
  }
  const pending = row?.taskStatus === 'PENDING'
  const running = row?.taskStatus === 'RUNNING'
  const confirmText = pending
    ? '确认立即执行该待执行任务？'
    : running
      ? '仅当本次执行已中断（超过 30 分钟）时才可回收重试，确认继续？'
      : '确认重新执行该失败任务？历史执行记录将保留。'
  const successText = pending ? '已提交执行' : '已提交重试'
  proxy.$modal.confirm(confirmText).then(() => retryReviewTask(row.taskId)).then(() => {
    proxy.$modal.msgSuccess(successText)
    getList()
  }).catch(() => {})
}

function onStatusChange(value) {
  if (value) queryParams.value.queueOnly = false
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() {
  proxy.resetForm('queryRef')
  dateRange.value = []
  queryParams.value.queueOnly = true
  handleQuery()
}

loadProjects()
// 首次进入：TagsView 写入 cachedViews 可能晚于页面首次渲染，未被 keep-alive 缓存的组件
// 不触发 onActivated，首次加载必须在 onMounted 完成；
// 重入 tab：keep-alive 不重跑 onMounted，工作台卡片 query 回填依赖 onActivated
onMounted(() => {
  applyRouteQuery()
  getList()
})
let firstActivated = true
onActivated(() => {
  // 首次挂载紧随的激活已在 onMounted 加载，跳过避免重复请求
  if (firstActivated) { firstActivated = false; return }
  applyRouteQuery()
  getList()
})

function applyRouteQuery() {
  const q = route.query || {}
  if (q.taskStatus) {
    queryParams.value.taskStatus = String(q.taskStatus)
    queryParams.value.queueOnly = false
  }
  if (q.projectId) queryParams.value.projectId = Number(q.projectId) || q.projectId
  if (q.prNumber) queryParams.value.prNumber = Number(q.prNumber) || q.prNumber
  if (q.eventSource) queryParams.value.eventSource = String(q.eventSource)
}
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.pr-cell { display: flex; align-items: center; gap: 8px; }
.pr-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.branch-flow { color: var(--el-text-color-regular); }
.failure-message { color: var(--el-color-danger); }
.empty-tip { color: var(--el-text-color-placeholder); }
.action-wrap { display: inline-block; }
</style>
