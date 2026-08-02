<template>
  <div class="app-container">
    <div class="page-toolbar">
      <el-page-header @back="goBack" content="审查任务详情" />
      <div class="toolbar-actions">
        <el-button v-if="detailTask?.taskStatus === 'SUCCESS' || detailTask?.taskStatus === 'FAILED'" type="primary" plain
          v-hasPermi="['review:record:query']"
          @click="goRecord">查看审查记录</el-button>
        <el-tooltip :content="retryActionTip(detailTask)" placement="top" :disabled="canRetry(detailTask)">
          <span class="action-wrap">
            <el-button type="primary" :disabled="!canRetry(detailTask)" @click="handleRetry">
              {{ retryActionLabel(detailTask) }}
            </el-button>
          </span>
        </el-tooltip>
      </div>
    </div>

    <el-alert v-if="detailError" :title="detailError" type="warning" :closable="false" show-icon class="mb12" />

    <div v-loading="detailLoading">
      <template v-if="detailTask">
        <section class="detail-section">
          <h4>任务概览</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="任务 ID">{{ detailTask.taskId }}</el-descriptions-item>
            <el-descriptions-item label="项目">{{ emptyDash(detailTask.projectName) }}</el-descriptions-item>
            <el-descriptions-item label="PR">#{{ detailTask.prNumber }} {{ emptyDash(detailTask.prTitle) }}</el-descriptions-item>
            <el-descriptions-item label="PR 发起人">{{ emptyDash(detailTask.prAuthor) }}</el-descriptions-item>
            <el-descriptions-item label="分支">{{ emptyDash(detailTask.sourceBranch) }} → {{ emptyDash(detailTask.targetBranch) }}</el-descriptions-item>
            <el-descriptions-item label="代码变更">{{ formatCodeChange(detailTask.changedFiles, detailTask.additions, detailTask.deletions) }}</el-descriptions-item>
            <el-descriptions-item label="任务状态">
              <dict-tag :options="review_task_status" :value="detailTask.taskStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="当前步骤">
              <dict-tag v-if="detailTask.currentStep" :options="review_task_step" :value="detailTask.currentStep" />
              <span v-else>—</span>
            </el-descriptions-item>
            <el-descriptions-item label="执行次数">{{ detailTask.attemptCount == null ? '—' : detailTask.attemptCount }}</el-descriptions-item>
            <el-descriptions-item label="耗时">{{ formatDuration(detailTask.durationMs) }}</el-descriptions-item>
            <el-descriptions-item label="审查方式">
              <dict-tag v-if="detailTask.snapshotReviewMode" :options="review_mode" :value="normalizeMode(detailTask.snapshotReviewMode)" />
              <span v-else>—</span>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(detailTask.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="失败分类" :span="2">
              <dict-tag v-if="detailTask.failureType" :options="review_task_failure_type" :value="detailTask.failureType" />
              <span v-else>—</span>
            </el-descriptions-item>
            <el-descriptions-item label="失败原因" :span="2">{{ emptyDash(detailTask.failureMessage) }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="detail-section">
          <h4>执行记录（排障）</h4>
          <p class="section-hint">仅展示每次 attempt 的技术信息。完整评分与重点问题请到审查记录查看。</p>
          <el-empty v-if="!detailRuns.length" description="暂无执行记录（待执行任务开始后会出现）" :image-size="64" />
          <el-table v-else :data="detailRuns" border>
            <el-table-column label="次数" prop="attemptNo" width="70" align="center" />
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <dict-tag :options="review_task_status" :value="scope.row.runStatus" />
              </template>
            </el-table-column>
            <el-table-column label="模型 / 引擎" min-width="160">
              <template #default="scope">{{ engineOrModelLabel(scope.row) }}</template>
            </el-table-column>
            <el-table-column label="模板" min-width="140">
              <template #default="scope">{{ templateLabel(scope.row) }}</template>
            </el-table-column>
            <el-table-column label="SHA" min-width="150">
              <template #default="scope">{{ shortSha(scope.row.snapshotBaseSha) }} → {{ shortSha(scope.row.snapshotHeadSha) }}</template>
            </el-table-column>
            <el-table-column label="耗时" width="100">
              <template #default="scope">{{ formatDuration(scope.row.durationMs) }}</template>
            </el-table-column>
            <el-table-column label="失败原因" min-width="180">
              <template #default="scope">
                <span v-if="scope.row.failureMessage" class="failure-message">{{ scope.row.failureMessage }}</span>
                <span v-else class="empty-tip">—</span>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup name="ReviewTaskDetail">
import { getReviewTask, retryReviewTask } from '@/api/review/task'
import auth from '@/plugins/auth'
import {
  emptyDash, formatDuration, formatCodeChange, normalizeMode,
  shortSha, engineOrModelLabel, templateLabel
} from '@/utils/reviewDisplay'

const { proxy } = getCurrentInstance()
const route = useRoute()
const {
  review_task_status, review_task_step, review_task_failure_type, review_mode
} = proxy.useDict(
  'review_task_status', 'review_task_step', 'review_task_failure_type', 'review_mode'
)

const detailLoading = ref(false)
const detailTask = ref(null)
const detailRuns = ref([])
const detailError = ref('')
const pollTimer = ref(null)

const taskId = computed(() => route.params.taskId)

function loadDetail() {
  if (!taskId.value) {
    detailError.value = '缺少任务 ID'
    return
  }
  detailLoading.value = true
  detailError.value = ''
  getReviewTask(taskId.value).then(response => {
    const payload = response.data || {}
    detailTask.value = payload.task || null
    detailRuns.value = payload.runs || []
    if (!detailTask.value) detailError.value = '未获取到任务详情'
  }).catch(error => {
    detailError.value = error?.message || '详情加载失败'
  }).finally(() => {
    detailLoading.value = false
    schedulePolling()
  })
}

function schedulePolling() {
  clearPolling()
  if (detailTask.value?.taskStatus === 'RUNNING') {
    pollTimer.value = setTimeout(() => loadDetail(), 5000)
  }
}

function clearPolling() {
  if (pollTimer.value) { clearTimeout(pollTimer.value); pollTimer.value = null }
}

onBeforeUnmount(() => clearPolling())

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

function retryActionTip(row) {
  if (!row) return '加载中…'
  if (!auth.hasPermi('review:task:retry')) return '当前账号没有执行/重试权限'
  const status = row.taskStatus
  if (status === 'FAILED') return '可重试，历史执行记录会保留'
  if (status === 'PENDING') return '可手动触发执行'
  if (status === 'RUNNING') return '执行中；中断超过 30 分钟可回收重试'
  if (status === 'SUCCESS') return `状态为「${statusLabel(status)}」，请到审查记录查看结果`
  return '当前状态不可执行'
}

function handleRetry() {
  if (!canRetry(detailTask.value)) {
    proxy.$modal.msgWarning(retryActionTip(detailTask.value))
    return
  }
  const pending = detailTask.value.taskStatus === 'PENDING'
  proxy.$modal.confirm(pending ? '确认立即执行该待执行任务？' : '确认重试？历史执行记录将保留。')
    .then(() => retryReviewTask(detailTask.value.taskId))
    .then(() => {
      proxy.$modal.msgSuccess(pending ? '已提交执行' : '已提交重试')
      loadDetail()
    }).catch(() => {})
}

function goBack() {
  proxy.$router.push('/review/task')
}

function goRecord() {
  proxy.$router.push('/review/record-detail/index/' + detailTask.value.taskId)
}

function formatDateTime(value) {
  return value ? proxy.parseTime(value) : '—'
}

watch(taskId, () => loadDetail(), { immediate: true })
</script>

<style scoped>
.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.toolbar-actions { display: flex; align-items: center; gap: 8px; }
.mb12 { margin-bottom: 12px; }
.detail-section { margin-bottom: 28px; }
.detail-section h4 { margin: 0 0 12px; font-size: 15px; font-weight: 600; }
.section-hint { margin: -4px 0 12px; font-size: 13px; color: var(--el-text-color-secondary); }
.failure-message { color: var(--el-color-danger); }
.empty-tip { color: var(--el-text-color-placeholder); }
.action-wrap { display: inline-block; }
</style>
