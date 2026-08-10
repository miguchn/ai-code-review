<template>
  <section class="delivery-block detail-section">
    <div class="delivery-head">
      <h4>{{ isPush ? '代码平台评论' : '总结评论投递' }}</h4>
      <el-button
        v-if="canRetry"
        type="warning"
        plain
        size="small"
        v-hasPermi="['review:delivery:retry']"
        :loading="retrying"
        @click="onRetry"
      >{{ retryLabel }}</el-button>
    </div>
    <p v-if="isPush" class="section-hint">
      Push 审查没有可挂载的代码平台评论；审查结果通过通知和问题台账交付。
    </p>
    <p v-else class="section-hint">与审查结论独立：投递失败不影响任务成功状态。同一变更来源仅维护一条 ACR 总结评论。</p>
    <p v-if="isPush" class="section-hint">本任务无需投递代码平台评论。</p>
    <p v-else-if="!delivery" class="section-hint">
      {{ isMissingOnSuccess ? '尚未投递总结评论，可点击右上角「补投递」。' : '本任务未投递总结评论。' }}
    </p>
    <el-descriptions v-else-if="delivery" :column="2" border>
      <el-descriptions-item label="投递状态">
        <el-tag v-if="delivery.deliveryStatus === 'SUCCESS'" type="success" size="small">已投递</el-tag>
        <el-tag v-else-if="delivery.deliveryStatus === 'FAILED'" type="danger" size="small">投递失败</el-tag>
        <span v-else class="empty-tip">暂无数据</span>
      </el-descriptions-item>
      <el-descriptions-item label="最后尝试时间">
        {{ delivery.lastAttemptTime ? formatDateTime(delivery.lastAttemptTime) : '暂无数据' }}
      </el-descriptions-item>
      <el-descriptions-item label="尝试次数">
        {{ delivery.attemptCount == null ? '暂无数据' : delivery.attemptCount }}
      </el-descriptions-item>
      <el-descriptions-item label="外部评论 ID">
        {{ delivery.externalId || '暂无数据' }}
      </el-descriptions-item>
      <el-descriptions-item v-if="delivery.deliveryStatus === 'FAILED'" label="失败原因" :span="2">
        <span class="failure-message">{{ delivery.failureMessage || '未返回可读失败原因' }}</span>
      </el-descriptions-item>
    </el-descriptions>
  </section>
</template>

<script setup name="DeliveryStatusView">
import { retryReviewDelivery } from '@/api/review/delivery'
import { formatDateTime } from '@/utils/reviewDisplay'
import auth from '@/plugins/auth'

const props = defineProps({
  delivery: { type: Object, default: null },
  taskId: { type: [Number, String], default: null },
  /** 当前详情任务状态：SUCCESS 且无投递记录时可补投递 */
  taskStatus: { type: String, default: '' },
  eventSource: { type: String, default: '' }
})

const emit = defineEmits(['retried'])
const { proxy } = getCurrentInstance()
const retrying = ref(false)

const isPush = computed(() => props.eventSource === 'PUSH')
const isFailed = computed(() => props.delivery?.deliveryStatus === 'FAILED')
const isMissingOnSuccess = computed(() => !isPush.value && !props.delivery && props.taskStatus === 'SUCCESS')

const canRetry = computed(() => {
  return !isPush.value
    && props.taskId != null
    && auth.hasPermi('review:delivery:retry')
    && (isFailed.value || isMissingOnSuccess.value)
})

const retryLabel = computed(() => (isMissingOnSuccess.value ? '补投递' : '重试投递'))

function onRetry() {
  if (!canRetry.value) return
  const tip = isMissingOnSuccess.value
    ? '确认补投递总结评论？将按该变更来源最近一次成功审查结论创建或更新评论。'
    : '确认重试投递总结评论？将按该变更来源最近一次成功审查结论更新评论。'
  proxy.$modal.confirm(tip)
    .then(() => {
      retrying.value = true
      return retryReviewDelivery(props.taskId)
    })
    .then(() => {
      proxy.$modal.msgSuccess(isMissingOnSuccess.value ? '补投递已完成' : '投递重试已完成')
      emit('retried')
    })
    .catch(() => {})
    .finally(() => { retrying.value = false })
}
</script>

<style scoped>
.delivery-block { margin-bottom: 20px; }
.delivery-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}
.delivery-head h4 { margin: 0; font-size: 15px; font-weight: 600; }
.section-hint { margin: 0 0 12px; font-size: 13px; color: var(--el-text-color-secondary); }
.failure-message { color: var(--el-color-danger); }
.empty-tip { color: var(--el-text-color-placeholder); }
</style>
