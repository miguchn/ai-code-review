<template>
  <el-drawer
    :model-value="modelValue"
    direction="rtl"
    size="min(720px, calc(100vw - 32px))"
    append-to-body
    :before-close="handleBeforeClose"
    class="issue-detail-drawer"
  >
    <template #header>
      <div class="drawer-head">
        <span class="drawer-head-title">
          {{ detailIssue ? ('问题 #' + detailIssue.issueId + ' · ' + (detailIssue.title || '问题详情')) : '问题详情' }}
        </span>
        <dict-tag v-if="detailIssue?.status" :options="review_issue_status" :value="detailIssue.status" />
      </div>
    </template>

    <div v-loading="detailLoading" class="drawer-body">
      <template v-if="detailIssue">
        <section class="lifecycle-bar" aria-label="问题生命周期">
          <div
            v-for="(node, idx) in lifecycleNodes"
            :key="node.key"
            class="lifecycle-node"
            :class="{
              'is-reached': node.reached,
              'is-current': node.current,
              'is-last': idx === lifecycleNodes.length - 1
            }"
          >
            <div class="lifecycle-track">
              <span class="lifecycle-dot" />
              <span v-if="idx < lifecycleNodes.length - 1" class="lifecycle-line" />
            </div>
            <div class="lifecycle-label">{{ node.label }}</div>
            <div class="lifecycle-meta">
              <span v-if="node.timeText">{{ node.timeText }}</span>
              <span v-if="node.roundText" class="lifecycle-round">{{ node.roundText }}</span>
            </div>
          </div>
        </section>

        <el-alert
          class="lifecycle-hint"
          :type="lifecycleHint.type"
          :closable="false"
          show-icon
          :title="lifecycleHint.title"
          :description="lifecycleHint.description"
        />

        <section class="detail-section">
          <el-descriptions :column="2" border size="small" class="context-descriptions">
            <el-descriptions-item label="所属项目">{{ emptyDash(detailIssue.projectName) }}</el-descriptions-item>
            <el-descriptions-item label="业务系统">{{ emptyDash(detailIssue.businessSystemName) }}</el-descriptions-item>
            <el-descriptions-item label="变更来源">
              <el-button v-if="detailMergeRequestUrl" link type="primary" @click="openMergeRequest">
                {{ mergeRequestLabel(detailIssue.provider) }} #{{ detailIssue.prNumber }} · {{ emptyDash(detailIssue.prTitle) }}
              </el-button>
              <span v-else>{{ changeSourceLabel }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="发起人">{{ emptyDash(detailIssue.prAuthor) }}</el-descriptions-item>
            <el-descriptions-item label="分支">{{ branchLabel }}</el-descriptions-item>
            <el-descriptions-item label="最近提交"><code>{{ shortSha(detailIssue.headSha || detailIssue.lastSeenHeadSha) }}</code></el-descriptions-item>
            <el-descriptions-item label="发现时间">{{ formatDateTime(detailIssue.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="阶段进入">{{ formatDateTime(detailIssue.stageEnteredTime) }}</el-descriptions-item>
            <el-descriptions-item v-if="detailIssue.closedTime" label="关闭时间">
              {{ formatDateTime(detailIssue.closedTime) }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detailIssue.closeSource" label="关闭来源">
              {{ closeSourceLabel(detailIssue.closeSource) }}
            </el-descriptions-item>
          </el-descriptions>
          <div v-if="detailIssue.category" class="detail-category">分类：{{ detailIssue.category }}</div>
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

        <section v-if="detailFirstTask || detailLastTask" class="detail-block">
          <div class="detail-block-title">关联审查记录</div>
          <div class="review-record-links">
            <div v-if="detailFirstTask" class="source-task">
              <span class="record-link-label">首次发现</span>
              <el-button link type="primary" @click="goTask(detailFirstTask)">审查记录 #{{ detailFirstTask.taskId }}</el-button>
              <span class="source-task-time">{{ formatDateTime(detailFirstTask.finishedTime) }}</span>
            </div>
            <div v-if="detailLastTask" class="source-task">
              <span class="record-link-label">最近审查</span>
              <el-button link type="primary" @click="goTask(detailLastTask)">审查记录 #{{ detailLastTask.taskId }}</el-button>
              <span class="source-task-time">{{ formatDateTime(detailLastTask.finishedTime) }}</span>
            </div>
          </div>
        </section>

        <section v-if="detailIssue.status === 'RECHECKING'" class="detail-block">
          <div class="detail-block-title">复核证据</div>
          <div class="recheck-evidence">
            <div class="recheck-row">
              <span class="recheck-label">未命中轮次</span>
              <el-button v-if="detailIssue.recheckTaskId" link type="primary" @click="goRecheckTask">
                任务 #{{ detailIssue.recheckTaskId }}
              </el-button>
              <span v-else class="empty-tip">—</span>
            </div>
            <div class="recheck-row">
              <span class="recheck-label">Commit</span>
              <code v-if="detailIssue.recheckCommitSha">{{ shortSha(detailIssue.recheckCommitSha) }}</code>
              <span v-else class="empty-tip">—</span>
            </div>
            <div class="recheck-row">
              <span class="recheck-label">连续未命中轮数</span>
              <span>{{ detailIssue.missedStreak == null ? '—' : detailIssue.missedStreak }}</span>
            </div>
            <div v-if="relatedIssues.length" class="recheck-row recheck-related">
              <span class="recheck-label">疑似关联新发现问题</span>
              <div class="related-links">
                <el-button
                  v-for="item in relatedIssues"
                  :key="item.issueId"
                  link
                  type="primary"
                  @click="openRelated(item.issueId)"
                >{{ item.title || ('#' + item.issueId) }}</el-button>
              </div>
            </div>
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
              <dict-tag
                v-if="detailSummaryDelivery.triggerSource"
                :options="review_delivery_trigger_source"
                :value="detailSummaryDelivery.triggerSource"
              />
              <span v-if="detailSummaryDelivery.lastAttemptTime" class="delivery-time">
                {{ formatDateTime(detailSummaryDelivery.lastAttemptTime) }}
              </span>
            </div>
            <p
              v-if="detailSummaryDelivery.deliveryStatus === 'FAILED'"
              class="delivery-failure"
              :title="detailSummaryDelivery.failureMessage || ''"
            >
              {{ detailSummaryDelivery.failureMessage || '—' }}
            </p>
          </template>
          <span v-else class="empty-tip">暂无该总结评论投递记录</span>
        </section>

        <section class="detail-block">
          <div class="detail-block-title">生命周期时间线</div>
          <el-empty v-if="!detailActions.length" description="暂无处置记录" :image-size="48" />
          <el-timeline v-else class="action-timeline">
            <el-timeline-item
              v-for="action in detailActions"
              :key="action.actionId"
              :timestamp="formatDateTime(action.createTime)"
              placement="top"
            >
              <div class="action-item">
                <el-tag :type="actionSourceTagType(action)" size="small" effect="plain" class="action-source">
                  {{ actionSourceLabel(action) }}
                </el-tag>
                <span class="action-type">{{ actionTypeLabel(action) }}</span>
                <span class="action-operator">{{ operatorLabel(action.operator) }}</span>
                <div
                  v-if="showStatusPair(action) && (action.fromStatus || action.toStatus)"
                  class="action-status"
                >
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
          <el-button
            v-if="detailIssue.status === 'AWAITING_CONFIRM'"
            type="primary"
            v-hasPermi="['review:issue:confirm']"
            :loading="actionLoading"
            @click="handleConfirm"
          >确认</el-button>
          <template v-if="isOpenStatus(detailIssue.status)">
            <el-button v-hasPermi="['review:issue:close']" :loading="actionLoading" @click="openCloseDialog">关闭</el-button>
            <el-button v-hasPermi="['review:issue:close']" :loading="actionLoading" @click="openDismissDialog('IGNORED')">忽略</el-button>
            <el-button v-hasPermi="['review:issue:close']" :loading="actionLoading" @click="openDismissDialog('FALSE_POSITIVE')">误报</el-button>
          </template>
          <template v-if="detailIssue.status === 'RECHECKING'">
            <el-button
              type="primary"
              v-hasPermi="['review:issue:close']"
              :loading="actionLoading"
              @click="openCloseDialog"
            >确认已修复并关闭</el-button>
            <el-button v-hasPermi="['review:issue:close']" :loading="actionLoading" @click="handleReopen">未修复，重新打开</el-button>
          </template>
        </div>
      </template>
    </div>
  </el-drawer>

  <el-dialog v-model="closeDialogVisible" :title="closeDialogTitle" width="480px" append-to-body>
    <el-form ref="closeFormRef" :model="closeForm" label-width="88px">
      <el-form-item label="关闭说明">
        <el-input
          v-model="closeForm.resolveNote"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="可选：说明关闭原因或修复方式"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitClose">{{ closeSubmitLabel }}</el-button>
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
        <el-input
          v-model="dismissForm.resolveNote"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="必填：说明忽略或误报原因"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dismissDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitDismiss">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { listIssue, getIssue, confirmIssue, closeIssue, dismissIssue, reopenIssue } from '@/api/review/issue'
import auth from '@/plugins/auth'
import {
  emptyDash, formatDateTime, formatIssueLines, shortSha, isPushTask,
  buildMergeRequestUrl, formatPushRefDisplay, mergeRequestLabel
} from '@/utils/reviewDisplay'
import { buildLifecycleNodes } from './issueLifecycle'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  issueId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:modelValue', 'update:issueId', 'disposed'])

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

const OPEN_STATUSES = ['AWAITING_CONFIRM', 'AWAITING_FIX']
const ACTION_TYPE_LABELS = {
  CONFIRM: '确认问题',
  CLOSE: '关闭问题',
  DISMISS: '忽略/误报',
  AUTO_RECHECK: '自动转复核',
  AUTO_REOPEN: '自动重开',
  REOPEN: '重新打开',
  DETECTED: '发现',
  ROUND_HIT: '再次命中',
  ROUND_MISS: '未命中'
}

const detailLoading = ref(false)
const detailIssue = ref(null)
const detailFirstTask = ref(null)
const detailLastTask = ref(null)
const detailActions = ref([])
const detailSummaryDelivery = ref(null)
const relatedIssues = ref([])
const actionLoading = ref(false)

const closeDialogVisible = ref(false)
const closeForm = ref({ resolveNote: '' })
const dismissDialogVisible = ref(false)
const dismissForm = ref({ dismissType: 'IGNORED', resolveNote: '' })
const dismissRules = {
  dismissType: [{ required: true, message: '请选择处置类型', trigger: 'change' }],
  resolveNote: [{ required: true, message: '请填写原因说明', trigger: 'blur' }]
}

const dismissDialogTitle = computed(() => {
  return dismissForm.value.dismissType === 'FALSE_POSITIVE' ? '标记误报' : '忽略问题'
})

const closeDialogTitle = computed(() => {
  return detailIssue.value?.status === 'RECHECKING' ? '确认已修复并关闭' : '关闭问题'
})

const closeSubmitLabel = computed(() => {
  return detailIssue.value?.status === 'RECHECKING' ? '确认关闭' : '确定关闭'
})

const showDetailActions = computed(() => {
  const status = detailIssue.value?.status
  return status === 'AWAITING_CONFIRM' || isOpenStatus(status) || status === 'RECHECKING'
})

const canOpenDeliveryList = computed(() => auth.hasPermi('review:delivery:list'))

const lifecycleNodes = computed(() => buildLifecycleNodes(detailIssue.value, detailActions.value, formatDateTime))
const detailMergeRequestUrl = computed(() => buildMergeRequestUrl(detailLastTask.value || detailIssue.value || {}))
const changeSourceLabel = computed(() => {
  const issue = detailIssue.value
  if (!issue) return '—'
  if (isPushIssue(issue)) return `Push ${formatPushRefDisplay(issue)}`
  return `${mergeRequestLabel(issue.provider)} #${issue.prNumber} · ${emptyDash(issue.prTitle)}`
})
const branchLabel = computed(() => {
  const issue = detailIssue.value
  if (!issue) return '—'
  if (isPushIssue(issue)) return issue.targetBranch || issue.refBranch || issue.sourceBranch || '—'
  return `${emptyDash(issue.sourceBranch)} → ${emptyDash(issue.targetBranch)}`
})
const lifecycleHint = computed(() => {
  const status = detailIssue.value?.status
  const hints = {
    AWAITING_CONFIRM: {
      type: 'info', title: '系统已发现，等待人工确认', description: '请判断该问题是否需要进入修复治理；确认后将转为待修复。'
    },
    AWAITING_FIX: {
      type: 'warning', title: '问题已确认，等待代码修复', description: '下一次审查会继续跟踪是否命中；未命中达到阈值后进入待复核。'
    },
    RECHECKING: {
      type: 'warning', title: '疑似已修复，等待人工验证', description: '系统仅确认新代码中未再次命中，尚不能替代人工验证；请确认关闭或恢复为待修复。'
    },
    CLOSED: {
      type: 'success', title: '问题已关闭', description: '问题已完成验证或随合并请求关闭，可通过时间线追溯处置过程。'
    },
    IGNORED: {
      type: 'info', title: '问题已忽略', description: '该问题不再进入活跃治理，原因可在处置说明和时间线中查看。'
    },
    FALSE_POSITIVE: {
      type: 'info', title: '问题已标记为误报', description: '该问题不再进入活跃治理，原因可在处置说明和时间线中查看。'
    }
  }
  return hints[status] || { type: 'info', title: '问题生命周期', description: '通过审查记录和处置时间线追溯问题变化。' }
})

watch(() => [props.modelValue, props.issueId], ([visible, issueId]) => {
  if (visible && issueId) {
    loadDetail(issueId)
  }
  if (!visible) {
    resetDetail()
  }
})

function isPushIssue(issue) {
  return Number(issue?.prNumber) === 0 || isPushTask(issue)
}

function isOpenStatus(status) {
  return OPEN_STATUSES.includes(status)
}

function actionTypeLabel(action) {
  return ACTION_TYPE_LABELS[action?.actionType] || action?.actionType || '处置'
}

function operatorLabel(operator) {
  if (!operator) return ''
  if (String(operator).toLowerCase() === 'system') return '系统'
  return operator
}

function showStatusPair(action) {
  if (!action) return false
  return action.fromStatus !== action.toStatus
}

function isSystemAction(action) {
  if (String(action?.operator || '').toLowerCase() === 'system') return true
  return ['AUTO_RECHECK', 'AUTO_REOPEN', 'DETECTED', 'ROUND_HIT', 'ROUND_MISS'].includes(action?.actionType)
}

function actionSourceLabel(action) {
  return isSystemAction(action) ? '系统事件' : '人工处置'
}

function actionSourceTagType(action) {
  return isSystemAction(action) ? 'info' : 'primary'
}

function closeSourceLabel(source) {
  const labels = {
    manual: '人工关闭',
    auto_recheck: '复核确认关闭',
    pr_merged: '合并请求已合并',
    pr_closed: '合并请求已关闭'
  }
  return labels[source] || source || '—'
}

function resetDetail() {
  detailIssue.value = null
  detailFirstTask.value = null
  detailLastTask.value = null
  detailActions.value = []
  detailSummaryDelivery.value = null
  relatedIssues.value = []
}

function loadDetail(issueId) {
  if (!issueId) return
  detailLoading.value = true
  resetDetail()
  getIssue(issueId).then(response => {
    const payload = response.data || {}
    detailIssue.value = payload.issue || null
    detailFirstTask.value = payload.firstTask
      || (payload.issue?.firstTaskId === payload.issue?.lastTaskId ? payload.sourceTask : null)
    detailLastTask.value = payload.lastTask || payload.sourceTask || null
    detailActions.value = payload.actions || []
    detailSummaryDelivery.value = payload.summaryDelivery || null
    if (detailIssue.value?.status === 'RECHECKING') {
      loadRelatedIssues(detailIssue.value)
    }
  }).catch(error => {
    proxy.$modal.msgError(error?.message || '详情加载失败')
    handleBeforeClose()
  }).finally(() => { detailLoading.value = false })
}

function loadRelatedIssues(issue) {
  if (!issue?.prNumber || !issue?.filePath) {
    relatedIssues.value = []
    return
  }
  listIssue({
    pageNum: 1,
    pageSize: 10,
    prNumber: issue.prNumber,
    keyword: issue.filePath,
    severity: issue.severity || undefined,
    activeFlag: 'Y'
  }).then(response => {
    relatedIssues.value = (response.rows || [])
      .filter(row => row.issueId !== issue.issueId)
      .slice(0, 3)
  }).catch(() => {
    relatedIssues.value = []
  })
}

function handleBeforeClose(done) {
  emit('update:modelValue', false)
  emit('update:issueId', null)
  if (typeof done === 'function') done()
}

function openRelated(issueId) {
  emit('update:issueId', issueId)
}

function goTask(task) {
  const taskId = task?.taskId
  if (!taskId) return
  proxy.$router.push('/review/record-detail/index/' + taskId)
}

function openMergeRequest() {
  if (detailMergeRequestUrl.value) {
    window.open(detailMergeRequestUrl.value, '_blank', 'noopener,noreferrer')
  }
}

function goRecheckTask() {
  const taskId = detailIssue.value?.recheckTaskId
  if (!taskId) return
  proxy.$router.push('/review/record-detail/index/' + taskId)
}

function goDeliveryList() {
  router.push('/notify/delivery')
}

function reloadDetailAndList() {
  if (props.issueId) loadDetail(props.issueId)
  emit('disposed')
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
    return confirmIssue(props.issueId)
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
  const wasRechecking = detailIssue.value?.status === 'RECHECKING'
  actionLoading.value = true
  closeIssue(props.issueId, { resolveNote: closeForm.value.resolveNote || undefined }).then(response => {
    proxy.$modal.msgSuccess(wasRechecking ? '已确认修复并关闭' : '已关闭')
    notifyCommentSync(response)
    closeDialogVisible.value = false
    reloadDetailAndList()
  }).catch(() => {}).finally(() => { actionLoading.value = false })
}

function handleReopen() {
  proxy.$modal.confirm('确认该问题未修复，重新打开为待修复？').then(() => {
    actionLoading.value = true
    return reopenIssue(props.issueId)
  }).then(response => {
    proxy.$modal.msgSuccess('已重新打开')
    notifyCommentSync(response)
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
    dismissIssue(props.issueId, { ...dismissForm.value }).then(response => {
      proxy.$modal.msgSuccess('处置成功')
      notifyCommentSync(response)
      dismissDialogVisible.value = false
      reloadDetailAndList()
    }).catch(() => {}).finally(() => { actionLoading.value = false })
  })
}
</script>

<style scoped>
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
  color: var(--el-text-color-primary);
}
.drawer-body { padding: 0 4px 24px; }
.lifecycle-hint { margin: -8px 0 16px; }

.lifecycle-bar {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  padding: 12px 8px 8px;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}
.lifecycle-node {
  flex: 1;
  min-width: 0;
  text-align: center;
}
.lifecycle-track {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 16px;
  margin-bottom: 6px;
}
.lifecycle-dot {
  position: relative;
  z-index: 1;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid var(--el-border-color);
  background: var(--el-bg-color);
  box-sizing: border-box;
}
.lifecycle-node.is-reached .lifecycle-dot {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
}
.lifecycle-node.is-current .lifecycle-dot {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 3px var(--el-color-primary-light-7);
}
.lifecycle-node.is-current:not(.is-reached) .lifecycle-dot {
  background: var(--el-bg-color);
}
.lifecycle-line {
  position: absolute;
  left: 50%;
  right: -50%;
  top: 50%;
  height: 2px;
  margin-top: -1px;
  background: var(--el-border-color-lighter);
}
.lifecycle-node.is-reached:not(.is-last) .lifecycle-line {
  background: var(--el-color-primary-light-5);
}
.lifecycle-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
  line-height: 18px;
}
.lifecycle-node.is-current .lifecycle-label,
.lifecycle-node.is-reached .lifecycle-label {
  color: var(--el-text-color-primary);
}
.lifecycle-meta {
  min-height: 16px;
  margin-top: 2px;
  font-size: 11px;
  line-height: 16px;
  color: var(--el-text-color-placeholder);
}
.lifecycle-round { margin-left: 4px; }

.detail-section { margin-bottom: 16px; }
.context-descriptions { margin-bottom: 12px; }
.context-descriptions code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}
.detail-category { margin-bottom: 8px; font-size: 12px; color: var(--el-text-color-secondary); }
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
  color: var(--el-text-color-regular);
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
  color: var(--el-text-color-primary);
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
  color: var(--el-text-color-regular);
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
  color: var(--el-text-color-regular);
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
.review-record-links { display: flex; flex-direction: column; gap: 8px; }
.record-link-label { min-width: 58px; color: var(--el-text-color-secondary); }
.recheck-evidence {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
}
.recheck-row {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
}
.recheck-label {
  min-width: 112px;
  color: var(--el-text-color-secondary);
}
.recheck-evidence code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}
.related-links {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  min-width: 0;
}
.action-timeline { padding-left: 4px; }
.action-item { font-size: 13px; }
.action-type { font-weight: 500; margin-right: 8px; color: var(--el-text-color-primary); }
.action-source { margin-right: 8px; }
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
