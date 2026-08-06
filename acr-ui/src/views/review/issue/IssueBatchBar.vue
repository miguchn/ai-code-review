<template>
  <div v-if="selectedRows.length" class="issue-batch-bar">
    <span class="batch-count">已选 {{ selectedRows.length }} 项</span>
    <span class="batch-sep">｜</span>
    <el-tooltip :disabled="confirmEnabled" :content="confirmDisabledTip" placement="top">
      <span class="batch-btn-wrap">
        <el-button
          link
          type="primary"
          v-hasPermi="['review:issue:confirm']"
          :disabled="!confirmEnabled"
          :loading="loading"
          @click="handleConfirm"
        >确认</el-button>
      </span>
    </el-tooltip>
    <el-tooltip :disabled="closeEnabled" :content="closeDisabledTip" placement="top">
      <span class="batch-btn-wrap">
        <el-button
          link
          type="primary"
          v-hasPermi="['review:issue:close']"
          :disabled="!closeEnabled"
          :loading="loading"
          @click="openCloseDialog"
        >关闭</el-button>
      </span>
    </el-tooltip>
    <el-tooltip :disabled="dismissEnabled" :content="dismissDisabledTip" placement="top">
      <span class="batch-btn-wrap">
        <el-button
          link
          type="primary"
          v-hasPermi="['review:issue:close']"
          :disabled="!dismissEnabled"
          :loading="loading"
          @click="openDismissDialog('IGNORED')"
        >忽略</el-button>
      </span>
    </el-tooltip>
    <el-tooltip :disabled="dismissEnabled" :content="dismissDisabledTip" placement="top">
      <span class="batch-btn-wrap">
        <el-button
          link
          type="primary"
          v-hasPermi="['review:issue:close']"
          :disabled="!dismissEnabled"
          :loading="loading"
          @click="openDismissDialog('FALSE_POSITIVE')"
        >误报</el-button>
      </span>
    </el-tooltip>
    <span class="batch-sep">｜</span>
    <el-button link @click="emit('clear')">清除选择</el-button>
  </div>

  <el-dialog v-model="closeDialogVisible" :title="closeDialogTitle" width="480px" append-to-body>
    <el-form :model="closeForm" label-width="88px">
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
      <el-button type="primary" :loading="loading" @click="submitClose">确定关闭</el-button>
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
      <el-button type="primary" :loading="loading" @click="submitDismiss">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ElMessageBox } from 'element-plus'
import { batchDisposeIssue } from '@/api/review/issue'
import auth from '@/plugins/auth'

const props = defineProps({
  selectedRows: { type: Array, default: () => [] }
})

const emit = defineEmits(['clear', 'done'])

const { proxy } = getCurrentInstance()

const CLOSE_STATUSES = ['AWAITING_CONFIRM', 'AWAITING_FIX', 'RECHECKING']
const DISMISS_STATUSES = ['AWAITING_CONFIRM', 'AWAITING_FIX']

const loading = ref(false)
const closeDialogVisible = ref(false)
const closeForm = ref({ resolveNote: '' })
const dismissDialogVisible = ref(false)
const dismissForm = ref({ dismissType: 'IGNORED', resolveNote: '' })
const dismissRules = {
  dismissType: [{ required: true, message: '请选择处置类型', trigger: 'change' }],
  resolveNote: [{ required: true, message: '请填写原因说明', trigger: 'blur' }]
}

const confirmBlockCount = computed(() =>
  props.selectedRows.filter(row => row.status !== 'AWAITING_CONFIRM').length
)
const closeBlockCount = computed(() =>
  props.selectedRows.filter(row => !CLOSE_STATUSES.includes(row.status)).length
)
const dismissBlockCount = computed(() =>
  props.selectedRows.filter(row => !DISMISS_STATUSES.includes(row.status)).length
)

const confirmEnabled = computed(() =>
  props.selectedRows.length > 0 && confirmBlockCount.value === 0
)
const closeEnabled = computed(() =>
  props.selectedRows.length > 0 && closeBlockCount.value === 0
)
const dismissEnabled = computed(() =>
  props.selectedRows.length > 0 && dismissBlockCount.value === 0
)

const confirmDisabledTip = computed(() =>
  `选中项含不可确认的问题（${confirmBlockCount.value} 项）`
)
const closeDisabledTip = computed(() =>
  `选中项含不可关闭的问题（${closeBlockCount.value} 项）`
)
const dismissDisabledTip = computed(() =>
  `选中项含不可忽略/误报的问题（${dismissBlockCount.value} 项）`
)

const closeDialogTitle = computed(() => `关闭 ${props.selectedRows.length} 个问题`)
const dismissDialogTitle = computed(() => {
  const n = props.selectedRows.length
  return dismissForm.value.dismissType === 'FALSE_POSITIVE'
    ? `标记 ${n} 个问题为误报`
    : `忽略 ${n} 个问题`
})

function issueIds() {
  return props.selectedRows.map(row => row.issueId).filter(id => id != null)
}

function notifyCommentSync(response) {
  const data = response?.data || {}
  if (data.commentSyncStatus !== 'FAILED') return
  const reason = data.commentSyncFailureMessage
  const msg = reason
    ? `评论同步失败：${reason}`
    : '评论同步失败，可在投递记录重试'
  if (auth.hasPermi('review:delivery:list')) {
    proxy.$modal.confirm(msg + '。是否前往投递记录？').then(() => {
      proxy.$router.push('/notify/delivery')
    }).catch(() => {})
  } else {
    proxy.$modal.msgWarning(msg)
  }
}

function showFailures(failures) {
  const list = Array.isArray(failures) ? failures : []
  const lines = list.map(item => {
    const title = item?.title || ('#' + (item?.issueId ?? ''))
    const reason = item?.reason || '不满足处置条件'
    return `${title} — ${reason}`
  })
  const html = lines.length
    ? `<div style="max-height:320px;overflow:auto;text-align:left;line-height:1.7">${lines.map(l => `<div>${escapeHtml(l)}</div>`).join('')}</div>`
    : '部分问题不满足处置条件'
  ElMessageBox.alert(html, '批量处置未执行', {
    dangerouslyUseHTMLString: true,
    confirmButtonText: '知道了',
    type: 'warning'
  }).catch(() => {})
}

function escapeHtml(text) {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function runBatch(payload) {
  loading.value = true
  return batchDisposeIssue(payload).then(response => {
    const data = response?.data || {}
    const n = data.successCount != null ? data.successCount : issueIds().length
    proxy.$modal.msgSuccess(`已成功处理 ${n} 项`)
    notifyCommentSync(response)
    closeDialogVisible.value = false
    dismissDialogVisible.value = false
    emit('done')
  }).catch(error => {
    const failures = error?.ajaxResult?.failures || error?.failures
    if (failures && failures.length) {
      showFailures(failures)
    }
  }).finally(() => { loading.value = false })
}

function handleConfirm() {
  if (!confirmEnabled.value) return
  proxy.$modal.confirm(`确认选中的 ${props.selectedRows.length} 个问题需要修复？`).then(() => {
    return runBatch({ action: 'CONFIRM', issueIds: issueIds() })
  }).catch(() => {})
}

function openCloseDialog() {
  if (!closeEnabled.value) return
  closeForm.value = { resolveNote: '' }
  closeDialogVisible.value = true
}

function submitClose() {
  runBatch({
    action: 'CLOSE',
    issueIds: issueIds(),
    resolveNote: closeForm.value.resolveNote || undefined
  })
}

function openDismissDialog(dismissType) {
  if (!dismissEnabled.value) return
  dismissForm.value = { dismissType, resolveNote: '' }
  dismissDialogVisible.value = true
  nextTick(() => proxy.resetForm('dismissFormRef'))
}

function submitDismiss() {
  proxy.$refs.dismissFormRef.validate(valid => {
    if (!valid) return
    runBatch({
      action: 'DISMISS',
      issueIds: issueIds(),
      dismissType: dismissForm.value.dismissType,
      resolveNote: dismissForm.value.resolveNote
    })
  })
}
</script>

<style scoped>
.issue-batch-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 8px;
  margin-bottom: 12px;
  padding: 10px 14px;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}
.batch-count {
  font-weight: 500;
  color: var(--el-text-color-primary);
}
.batch-sep {
  color: var(--el-text-color-placeholder);
  margin: 0 2px;
}
.batch-btn-wrap {
  display: inline-flex;
}
</style>
