<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
      <el-form-item label="所属项目" prop="projectId">
        <el-select v-model="queryParams.projectId" clearable filterable placeholder="请选择项目" style="width: 200px">
          <el-option v-for="item in projectOptions" :key="item.projectId" :label="item.projectName" :value="item.projectId" />
        </el-select>
      </el-form-item>
      <el-form-item label="问题状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="请选择状态" style="width: 140px">
          <el-option v-for="dict in review_issue_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="严重度" prop="severity">
        <el-select v-model="queryParams.severity" clearable placeholder="请选择严重度" style="width: 120px">
          <el-option v-for="item in severityOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="归属" prop="origin">
        <el-select v-model="queryParams.origin" clearable placeholder="请选择归属" style="width: 120px">
          <el-option v-for="dict in review_issue_origin" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="标题或文件路径" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="issueList" empty-text="暂无问题记录">
      <el-table-column label="项目名称" prop="projectName" min-width="150" :show-overflow-tooltip="true" />
      <el-table-column label="合并请求" width="90">
        <template #default="scope">#{{ scope.row.prNumber }}</template>
      </el-table-column>
      <el-table-column label="问题标题" prop="title" min-width="200" :show-overflow-tooltip="true" />
      <el-table-column label="严重度" width="90">
        <template #default="scope">
          <el-tag v-if="scope.row.severity" :type="severityTagType(scope.row.severity)" size="small">
            {{ severityLabel(scope.row.severity) }}
          </el-tag>
          <span v-else class="empty-tip">—</span>
        </template>
      </el-table-column>
      <el-table-column label="归属" width="90">
        <template #default="scope">
          <dict-tag :options="review_issue_origin" :value="scope.row.origin" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="scope">
          <dict-tag :options="review_issue_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="170">
        <template #default="scope">{{ formatDateTime(scope.row.updateTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" v-hasPermi="['review:issue:query']" @click="openDetail(scope.row.issueId)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="drawerVisible" direction="rtl" size="min(720px, calc(100vw - 32px))" append-to-body
      :before-close="closeDrawer" class="issue-detail-drawer">
      <template #header>
        <div class="drawer-head">
          <span class="drawer-head-title">{{ detailIssue?.title || '问题详情' }}</span>
          <dict-tag v-if="detailIssue?.status" :options="review_issue_status" :value="detailIssue.status" />
        </div>
      </template>

      <div v-loading="detailLoading" class="drawer-body">
        <template v-if="detailIssue">
          <section class="detail-section">
            <div class="detail-meta">
              <span>{{ emptyDash(detailIssue.projectName) }}</span>
              <span class="meta-sep">·</span>
              <span>合并请求 #{{ detailIssue.prNumber }}</span>
              <span v-if="detailIssue.category" class="meta-sep">·</span>
              <span v-if="detailIssue.category">{{ detailIssue.category }}</span>
            </div>
            <p v-if="detailIssue.description" class="detail-desc">{{ detailIssue.description }}</p>
          </section>

          <section class="detail-block">
            <div class="detail-block-title">定位</div>
            <div v-if="detailIssue.filePath || formatIssueLines(detailIssue)" class="issue-locate">
              <code v-if="detailIssue.filePath">{{ detailIssue.filePath }}</code>
              <span v-if="formatIssueLines(detailIssue)" class="issue-lines">{{ formatIssueLines(detailIssue) }}</span>
            </div>
            <span v-else class="empty-tip">—</span>
          </section>

          <section v-if="detailIssue.suggestion" class="detail-block">
            <div class="detail-block-title">建议</div>
            <p class="detail-text">{{ detailIssue.suggestion }}</p>
          </section>

          <section v-if="detailIssue.evidence" class="detail-block">
            <div class="detail-block-title">证据</div>
            <pre class="detail-pre">{{ detailIssue.evidence }}</pre>
          </section>

          <section class="detail-block">
            <div class="detail-block-title">归属</div>
            <dict-tag :options="review_issue_origin" :value="detailIssue.origin" />
          </section>

          <section v-if="detailSourceTask" class="detail-block">
            <div class="detail-block-title">来源任务</div>
            <div class="source-task">
              <el-button link type="primary" @click="goSourceTask">任务 #{{ detailSourceTask.taskId }}</el-button>
              <span v-if="detailSourceTask.prTitle" class="source-task-title">{{ detailSourceTask.prTitle }}</span>
              <span class="source-task-time">{{ formatDateTime(detailSourceTask.finishedTime) }}</span>
            </div>
          </section>

          <section class="detail-block">
            <div class="detail-block-title">
              <span>总结评论投递</span>
              <el-button v-if="canOpenDeliveryList" link type="primary" @click="goDeliveryList">查看投递记录</el-button>
            </div>
            <template v-if="detailSummaryDelivery">
              <div class="delivery-summary">
                <dict-tag :options="review_delivery_status" :value="detailSummaryDelivery.deliveryStatus" />
                <dict-tag v-if="detailSummaryDelivery.triggerSource" :options="review_delivery_trigger_source"
                  :value="detailSummaryDelivery.triggerSource" />
                <span v-if="detailSummaryDelivery.lastAttemptTime" class="delivery-time">
                  {{ formatDateTime(detailSummaryDelivery.lastAttemptTime) }}
                </span>
              </div>
              <p v-if="detailSummaryDelivery.deliveryStatus === 'FAILED'" class="delivery-failure"
                :title="detailSummaryDelivery.failureMessage || ''">
                {{ detailSummaryDelivery.failureMessage || '—' }}
              </p>
            </template>
            <span v-else class="empty-tip">暂无该总结评论投递记录</span>
          </section>

          <section class="detail-block">
            <div class="detail-block-title">动作时间线</div>
            <el-empty v-if="!detailActions.length" description="暂无处置记录" :image-size="48" />
            <el-timeline v-else class="action-timeline">
              <el-timeline-item v-for="action in detailActions" :key="action.actionId"
                :timestamp="formatDateTime(action.createTime)" placement="top">
                <div class="action-item">
                  <span class="action-type">{{ actionTypeLabel(action) }}</span>
                  <span v-if="action.operator" class="action-operator">{{ action.operator }}</span>
                  <div v-if="action.fromStatus || action.toStatus" class="action-status">
                    <dict-tag v-if="action.fromStatus" :options="review_issue_status" :value="action.fromStatus" />
                    <span v-if="action.fromStatus && action.toStatus" class="action-arrow">→</span>
                    <dict-tag v-if="action.toStatus" :options="review_issue_status" :value="action.toStatus" />
                  </div>
                  <p v-if="action.resolveNote" class="action-note">{{ action.resolveNote }}</p>
                </div>
              </el-timeline-item>
            </el-timeline>
          </section>

          <div v-if="showDetailActions" class="drawer-actions">
            <el-button v-if="detailIssue.status === 'AWAITING_CONFIRM'" type="primary"
              v-hasPermi="['review:issue:confirm']" :loading="actionLoading" @click="handleConfirm">确认</el-button>
            <template v-if="isOpenStatus(detailIssue.status)">
              <el-button v-hasPermi="['review:issue:close']" :loading="actionLoading" @click="openCloseDialog">关闭</el-button>
              <el-button v-hasPermi="['review:issue:close']" :loading="actionLoading" @click="openDismissDialog('IGNORED')">忽略</el-button>
              <el-button v-hasPermi="['review:issue:close']" :loading="actionLoading" @click="openDismissDialog('FALSE_POSITIVE')">误报</el-button>
            </template>
          </div>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="closeDialogVisible" title="关闭问题" width="480px" append-to-body>
      <el-form ref="closeFormRef" :model="closeForm" label-width="88px">
        <el-form-item label="关闭说明">
          <el-input v-model="closeForm.resolveNote" type="textarea" :rows="3" maxlength="500" show-word-limit
            placeholder="可选：说明关闭原因或修复方式" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitClose">确定关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dismissDialogVisible" :title="dismissDialogTitle" width="480px" append-to-body>
      <el-form ref="dismissFormRef" :model="dismissForm" :rules="dismissRules" label-width="88px">
        <el-form-item label="处置类型" prop="dismissType">
          <el-select v-model="dismissForm.dismissType" style="width: 100%">
            <el-option label="忽略" value="IGNORED" />
            <el-option label="误报" value="FALSE_POSITIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因说明" prop="resolveNote">
          <el-input v-model="dismissForm.resolveNote" type="textarea" :rows="3" maxlength="500" show-word-limit
            placeholder="必填：说明忽略或误报原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dismissDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitDismiss">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ReviewIssue">
import { useRoute, useRouter } from 'vue-router'
import { listIssue, getIssue, confirmIssue, closeIssue, dismissIssue } from '@/api/review/issue'
import { listReviewProject } from '@/api/review/project'
import auth from '@/plugins/auth'
import {
  emptyDash, formatDateTime, severityLabel, severityTagType, formatIssueLines
} from '@/utils/reviewDisplay'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()
const {
  review_issue_status,
  review_issue_origin,
  review_delivery_status,
  review_delivery_trigger_source
} = proxy.useDict(
  'review_issue_status',
  'review_issue_origin',
  'review_delivery_status',
  'review_delivery_trigger_source'
)

const severityOptions = [
  { label: '严重', value: 'CRITICAL' },
  { label: '高', value: 'HIGH' },
  { label: '中', value: 'MEDIUM' },
  { label: '低', value: 'LOW' },
  { label: '信息', value: 'INFO' }
]

const OPEN_STATUSES = ['AWAITING_CONFIRM', 'AWAITING_FIX']
const ACTION_TYPE_LABELS = {
  CONFIRM: '确认问题',
  CLOSE: '关闭问题',
  DISMISS: '忽略/误报'
}

const issueList = ref([])
const projectOptions = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const drawerVisible = ref(false)
const detailLoading = ref(false)
const detailIssue = ref(null)
const detailSourceTask = ref(null)
const detailActions = ref([])
const detailSummaryDelivery = ref(null)
const activeIssueId = ref(null)
const actionLoading = ref(false)

const closeDialogVisible = ref(false)
const closeForm = ref({ resolveNote: '' })

const dismissDialogVisible = ref(false)
const dismissForm = ref({ dismissType: 'IGNORED', resolveNote: '' })
const dismissRules = {
  dismissType: [{ required: true, message: '请选择处置类型', trigger: 'change' }],
  resolveNote: [{ required: true, message: '请填写原因说明', trigger: 'blur' }]
}

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  projectId: undefined,
  status: undefined,
  severity: undefined,
  origin: undefined,
  keyword: undefined
})

const dismissDialogTitle = computed(() => {
  return dismissForm.value.dismissType === 'FALSE_POSITIVE' ? '标记误报' : '忽略问题'
})

const showDetailActions = computed(() => {
  const status = detailIssue.value?.status
  return status === 'AWAITING_CONFIRM' || isOpenStatus(status)
})

const canOpenDeliveryList = computed(() => auth.hasPermi('review:delivery:list'))

function isOpenStatus(status) {
  return OPEN_STATUSES.includes(status)
}

function actionTypeLabel(action) {
  return ACTION_TYPE_LABELS[action?.actionType] || action?.actionType || '处置'
}

function getList() {
  loading.value = true
  listIssue(queryParams.value).then(response => {
    issueList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => { loading.value = false })
}

function loadProjects() {
  listReviewProject({ pageNum: 1, pageSize: 200, status: '0' }).then(response => {
    projectOptions.value = response.rows || []
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function openDetail(issueId) {
  activeIssueId.value = issueId
  drawerVisible.value = true
  loadDetail(issueId)
  syncIssueQuery(issueId)
}

function loadDetail(issueId) {
  if (!issueId) return
  detailLoading.value = true
  detailIssue.value = null
  detailSourceTask.value = null
  detailActions.value = []
  detailSummaryDelivery.value = null
  getIssue(issueId).then(response => {
    const payload = response.data || {}
    detailIssue.value = payload.issue || null
    detailSourceTask.value = payload.sourceTask || null
    detailActions.value = payload.actions || []
    detailSummaryDelivery.value = payload.summaryDelivery || null
  }).catch(error => {
    proxy.$modal.msgError(error?.message || '详情加载失败')
    closeDrawer()
  }).finally(() => { detailLoading.value = false })
}

function closeDrawer(done) {
  drawerVisible.value = false
  activeIssueId.value = null
  syncIssueQuery(undefined)
  if (typeof done === 'function') done()
}

function syncIssueQuery(issueId) {
  const next = { ...route.query }
  if (issueId) {
    next.issueId = String(issueId)
  } else {
    delete next.issueId
  }
  router.replace({ path: route.path, query: next })
}

function goSourceTask() {
  const taskId = detailSourceTask.value?.taskId
  if (!taskId) return
  proxy.$router.push('/review/record-detail/index/' + taskId)
}

function goDeliveryList() {
  router.push('/notify/delivery')
}

function reloadDetailAndList() {
  if (activeIssueId.value) loadDetail(activeIssueId.value)
  getList()
}

function notifyCommentSync(response) {
  const data = response?.data || {}
  if (data.commentSyncStatus !== 'FAILED') return
  const reason = data.commentSyncFailureMessage
  const msg = reason
    ? `评论同步失败：${reason}`
    : '评论同步失败，可在投递记录重试'
  if (canOpenDeliveryList.value) {
    proxy.$modal.confirm(msg + '。是否前往投递记录？').then(() => {
      goDeliveryList()
    }).catch(() => {})
  } else {
    proxy.$modal.msgWarning(msg)
  }
}

function handleConfirm() {
  proxy.$modal.confirm('确认该问题需要修复？').then(() => {
    actionLoading.value = true
    return confirmIssue(activeIssueId.value)
  }).then(response => {
    proxy.$modal.msgSuccess('已确认')
    notifyCommentSync(response)
    reloadDetailAndList()
  }).catch(() => {}).finally(() => { actionLoading.value = false })
}

function openCloseDialog() {
  closeForm.value = { resolveNote: '' }
  closeDialogVisible.value = true
}

function submitClose() {
  actionLoading.value = true
  closeIssue(activeIssueId.value, { resolveNote: closeForm.value.resolveNote || undefined }).then(response => {
    proxy.$modal.msgSuccess('已关闭')
    notifyCommentSync(response)
    closeDialogVisible.value = false
    reloadDetailAndList()
  }).catch(() => {}).finally(() => { actionLoading.value = false })
}

function openDismissDialog(dismissType) {
  dismissForm.value = { dismissType, resolveNote: '' }
  dismissDialogVisible.value = true
  nextTick(() => proxy.resetForm('dismissFormRef'))
}

function submitDismiss() {
  proxy.$refs.dismissFormRef.validate(valid => {
    if (!valid) return
    actionLoading.value = true
    dismissIssue(activeIssueId.value, { ...dismissForm.value }).then(response => {
      proxy.$modal.msgSuccess('处置成功')
      notifyCommentSync(response)
      dismissDialogVisible.value = false
      reloadDetailAndList()
    }).catch(() => {}).finally(() => { actionLoading.value = false })
  })
}

function openFromRoute() {
  const raw = route.query.issueId
  if (!raw) return
  const issueId = Number(raw)
  if (!issueId) return
  openDetail(issueId)
}

loadProjects()
watch(() => route.query.issueId, () => {
  if (route.query.issueId && !drawerVisible.value) openFromRoute()
})
onMounted(() => {
  openFromRoute()
  applyRouteQuery()
  getList()
})
// 首次进入：TagsView 写入 cachedViews 可能晚于页面首次渲染，未被 keep-alive 缓存的组件
// 不触发 onActivated，首次加载必须在 onMounted 完成；
// 重入 tab：keep-alive 不重跑 onMounted，工作台卡片 query 回填依赖 onActivated
let firstActivated = true
onActivated(() => {
  // 首次挂载紧随的激活已在 onMounted 加载，跳过避免重复请求
  if (firstActivated) { firstActivated = false; return }
  applyRouteQuery()
  getList()
})

function applyRouteQuery() {
  const q = route.query || {}
  if (q.status) queryParams.value.status = String(q.status)
  if (q.origin) queryParams.value.origin = String(q.origin)
  if (q.severity) queryParams.value.severity = String(q.severity)
  if (q.projectId) queryParams.value.projectId = Number(q.projectId) || q.projectId
  if (q.keyword) queryParams.value.keyword = String(q.keyword)
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.empty-tip { color: var(--el-text-color-placeholder); }
.drawer-head {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.drawer-head-title {
  font-size: 16px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.drawer-body { padding: 0 4px 24px; }
.detail-section { margin-bottom: 16px; }
.detail-meta {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}
.meta-sep { margin: 0 6px; }
.detail-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}
.detail-block { margin-bottom: 16px; }
.detail-block-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
}
.delivery-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.delivery-time { font-size: 12px; color: var(--el-text-color-secondary); }
.delivery-failure {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--el-color-danger);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.detail-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}
.detail-pre {
  margin: 0;
  padding: 10px 12px;
  max-height: 200px;
  overflow: auto;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
.issue-locate { font-size: 12px; color: var(--el-text-color-secondary); }
.issue-locate code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}
.issue-lines { margin-left: 8px; }
.source-task {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.source-task-title { color: var(--el-text-color-regular); }
.source-task-time { font-size: 12px; color: var(--el-text-color-secondary); }
.action-timeline { padding-left: 4px; }
.action-item { font-size: 13px; }
.action-type { font-weight: 500; margin-right: 8px; }
.action-operator { font-size: 12px; color: var(--el-text-color-secondary); }
.action-status {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
}
.action-arrow { color: var(--el-text-color-secondary); font-size: 12px; }
.action-note {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
  white-space: pre-wrap;
}
.drawer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
