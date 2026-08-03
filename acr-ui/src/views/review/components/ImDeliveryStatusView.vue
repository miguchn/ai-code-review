<template>
  <section v-if="visible" class="delivery-block detail-section">
    <div class="delivery-head">
      <h4>IM 群通知投递</h4>
      <router-link v-if="taskId" :to="deliveryListLink" class="delivery-link">查看投递记录</router-link>
    </div>
    <p v-if="loading" class="section-hint">加载 IM 投递状态…</p>
    <p v-else-if="!imDelivery" class="section-hint">本任务暂无 IM 群通知投递记录。</p>
    <div v-else class="im-summary">
      <dict-tag :options="review_delivery_channel" :value="imDelivery.channel" size="small" />
      <dict-tag :options="review_delivery_status" :value="imDelivery.deliveryStatus" size="small" />
      <span v-if="imDelivery.lastAttemptTime" class="attempt-time">{{ formatDateTime(imDelivery.lastAttemptTime) }}</span>
    </div>
  </section>
</template>

<script setup name="ImDeliveryStatusView">
import { getLatestImDelivery } from '@/api/review/delivery'
import { formatDateTime } from '@/utils/reviewDisplay'

const props = defineProps({
  taskId: { type: [Number, String], default: null }
})

const { proxy } = getCurrentInstance()
const { review_delivery_channel, review_delivery_status } = proxy.useDict('review_delivery_channel', 'review_delivery_status')

const loading = ref(false)
const imDelivery = ref(null)
const loaded = ref(false)

const visible = computed(() => props.taskId != null)
const deliveryListLink = computed(() => ({
  path: '/notify/delivery',
  query: props.taskId ? { taskId: String(props.taskId) } : undefined
}))

function loadImDelivery() {
  if (!props.taskId) {
    imDelivery.value = null
    loaded.value = false
    return
  }
  loading.value = true
  getLatestImDelivery(props.taskId).then(response => {
    imDelivery.value = response.data || null
  }).catch(() => {
    imDelivery.value = null
  }).finally(() => {
    loading.value = false
    loaded.value = true
  })
}

watch(() => props.taskId, () => loadImDelivery(), { immediate: true })
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
.delivery-link {
  font-size: 13px;
  color: var(--el-color-primary);
  text-decoration: none;
}
.delivery-link:hover { text-decoration: underline; }
.section-hint { margin: 0; font-size: 13px; color: var(--el-text-color-secondary); }
.im-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.attempt-time { font-size: 12px; color: var(--el-text-color-secondary); }
</style>
