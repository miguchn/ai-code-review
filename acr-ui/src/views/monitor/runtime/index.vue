<template>
  <div class="app-container runtime-page">
    <el-alert
      class="mb16"
      type="info"
      :closable="false"
      show-icon
      title="运行概览汇总审查调度、资源预算与投递队列状态。告警给出可执行处置建议，可跳转到任务或投递证据。"
    />

    <div class="toolbar">
      <el-button type="primary" icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
      <span v-if="updatedAt" class="updated">更新于 {{ updatedAt }}</span>
    </div>

    <div v-if="error" class="runtime-state">
      <span>{{ error }}</span>
      <el-button type="primary" link @click="loadAll">重试</el-button>
    </div>

    <template v-else>
      <el-row :gutter="16" class="mb16">
        <el-col :xs="24" :lg="8">
          <section class="surface-panel">
            <header class="panel-head"><h3>任务面</h3></header>
            <div class="metric-grid">
              <div class="metric"><span class="label">待执行</span><span class="value">{{ task.pendingCount ?? 0 }}</span></div>
              <div class="metric"><span class="label">待重试</span><span class="value">{{ task.retryingCount ?? 0 }}</span></div>
              <div class="metric"><span class="label">执行中</span><span class="value">{{ task.runningCount ?? 0 }}</span></div>
              <div class="metric"><span class="label">已被替代</span><span class="value">{{ task.supersededCount ?? 0 }}</span></div>
              <div class="metric wide">
                <span class="label">最老待执行年龄</span>
                <span class="value">{{ formatAge(task.oldestPendingAgeSeconds) }}</span>
                <el-button
                  v-if="task.oldestPendingTaskId"
                  link
                  type="primary"
                  @click="goTask(task.oldestPendingTaskId)"
                >任务 #{{ task.oldestPendingTaskId }}</el-button>
              </div>
              <div class="metric"><span class="label">近 24h 重试</span><span class="value">{{ task.retryCount24h ?? 0 }}</span></div>
              <div class="metric"><span class="label">近 24h 超时</span><span class="value">{{ task.timeoutCount24h ?? 0 }}</span></div>
            </div>
            <div class="terminal-row">
              <span v-for="(count, name) in task.terminalRatio24h || {}" :key="name" class="terminal-chip">
                {{ name }} {{ count }}
              </span>
            </div>
          </section>
        </el-col>

        <el-col :xs="24" :lg="8">
          <section class="surface-panel">
            <header class="panel-head"><h3>资源面</h3></header>
            <div class="metric-grid">
              <div class="metric"><span class="label">审查队列</span><span class="value">{{ resource.reviewQueueDepth ?? 0 }}/{{ resource.reviewQueueCapacity ?? 0 }}</span></div>
              <div class="metric"><span class="label">审查活跃线程</span><span class="value">{{ resource.reviewActiveCount ?? 0 }}/{{ resource.reviewPoolSize ?? 0 }}</span></div>
              <div class="metric"><span class="label">审查拒绝数</span><span class="value">{{ resource.reviewRejectedCount ?? 0 }}</span></div>
              <div class="metric"><span class="label">投递队列</span><span class="value">{{ resource.deliveryQueueDepth ?? 0 }}/{{ resource.deliveryQueueCapacity ?? 0 }}</span></div>
              <div class="metric"><span class="label">工作区占用</span><span class="value">{{ resource.workspaceHeld ?? 0 }}/{{ resource.workspaceLimit ?? 0 }}</span></div>
              <div class="metric"><span class="label">OCR 占用</span><span class="value">{{ resource.ocrHeld ?? 0 }}/{{ resource.ocrLimit ?? 0 }}</span></div>
              <div class="metric"><span class="label">LLM 占用</span><span class="value">{{ resource.llmHeld ?? 0 }}/{{ resource.llmLimit ?? 0 }}</span></div>
              <div class="metric"><span class="label">磁盘占用</span><span class="value">{{ resource.workspaceUsedMb ?? 0 }}/{{ resource.workspaceDiskLimitMb ?? 0 }} MB</span></div>
            </div>
          </section>
        </el-col>

        <el-col :xs="24" :lg="8">
          <section class="surface-panel">
            <header class="panel-head"><h3>交付面</h3></header>
            <div class="metric-grid">
              <div class="metric"><span class="label">待投递</span><span class="value">{{ delivery.pendingCount ?? 0 }}</span></div>
              <div class="metric"><span class="label">待人工处置</span><span class="value">{{ delivery.manualCount ?? 0 }}</span></div>
              <div class="metric wide">
                <span class="label">最老待投递年龄</span>
                <span class="value">{{ formatAge(delivery.oldestPendingAgeSeconds) }}</span>
                <el-button
                  v-if="delivery.oldestPendingDeliveryId"
                  link
                  type="primary"
                  @click="goDelivery(delivery.oldestPendingDeliveryId)"
                >投递 #{{ delivery.oldestPendingDeliveryId }}</el-button>
              </div>
            </div>
          </section>
        </el-col>
      </el-row>

      <section class="surface-panel mb16">
        <header class="panel-head">
          <h3>告警</h3>
          <span class="hint">内存 + 日志，阈值可在参数设置调整</span>
        </header>
        <el-empty v-if="!(alerts || []).length" description="当前无告警" :image-size="64" />
        <el-table v-else :data="alerts" empty-text="当前无告警">
          <el-table-column label="级别" width="90">
            <template #default="scope">
              <el-tag :type="scope.row.severity === 'critical' ? 'danger' : 'warning'" size="small">
                {{ scope.row.severity === 'critical' ? '严重' : '警告' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="标题" prop="title" width="140" />
          <el-table-column label="看哪里" prop="message" min-width="260" :show-overflow-tooltip="true" />
          <el-table-column label="怎么办" prop="action" min-width="220" :show-overflow-tooltip="true" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="scope">
              <el-button v-if="scope.row.taskId" link type="primary" @click="goTask(scope.row.taskId)">打开任务</el-button>
              <el-button v-if="scope.row.deliveryId" link type="primary" @click="goDelivery(scope.row.deliveryId)">打开投递</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="surface-panel">
        <header class="panel-head"><h3>积压与处置</h3></header>
        <el-tabs v-model="backlogTab" @tab-change="loadBacklog">
          <el-tab-pane label="超龄待执行" name="overdue" />
          <el-tab-pane label="租约过期" name="lease" />
          <el-tab-pane label="投递滞留" name="delivery" />
        </el-tabs>
        <el-table v-loading="backlogLoading" :data="backlogRows" empty-text="暂无积压项">
          <el-table-column label="类型" prop="statusLabel" width="120" />
          <el-table-column label="项目" prop="projectName" min-width="140" :show-overflow-tooltip="true" />
          <el-table-column label="摘要" prop="summary" min-width="220" :show-overflow-tooltip="true" />
          <el-table-column label="年龄" width="120">
            <template #default="scope">{{ formatAge(scope.row.ageSeconds) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="scope">
              <el-button v-if="scope.row.taskId" link type="primary" @click="goTask(scope.row.taskId)">任务</el-button>
              <el-button
                v-if="scope.row.runId"
                link
                type="primary"
                @click="goRecord(scope.row.taskId)"
              >运行记录</el-button>
              <el-button
                v-if="scope.row.deliveryId"
                link
                type="primary"
                @click="goDelivery(scope.row.deliveryId)"
              >投递</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>
  </div>
</template>

<script setup name="ReviewRuntimeOverview">
import {
  getRuntimeOverview,
  listOverduePendingTasks,
  listLeaseExpiredTasks,
  listStuckDeliveries
} from '@/api/review/runtime'
import { formatDateTime } from '@/utils/reviewDisplay'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const backlogLoading = ref(false)
const error = ref('')
const updatedAt = ref('')
const task = ref({})
const resource = ref({})
const delivery = ref({})
const alerts = ref([])
const backlogTab = ref('overdue')
const backlogRows = ref([])

function loadAll() {
  loading.value = true
  error.value = ''
  getRuntimeOverview().then(response => {
    const data = response.data || {}
    task.value = data.task || {}
    resource.value = data.resource || {}
    delivery.value = data.delivery || {}
    alerts.value = data.alerts || []
    updatedAt.value = formatDateTime(new Date())
  }).catch(err => {
    error.value = err?.message || '加载运行概览失败，请确认已授权「运行概览查看」'
  }).finally(() => {
    loading.value = false
    loadBacklog()
  })
}

function loadBacklog() {
  backlogLoading.value = true
  const loader = backlogTab.value === 'lease'
    ? listLeaseExpiredTasks(50)
    : backlogTab.value === 'delivery'
      ? listStuckDeliveries(50)
      : listOverduePendingTasks(50)
  loader.then(response => {
    backlogRows.value = response.data || []
  }).catch(() => {
    backlogRows.value = []
  }).finally(() => {
    backlogLoading.value = false
  })
}

function formatAge(seconds) {
  if (seconds == null) return '—'
  const value = Number(seconds)
  if (!Number.isFinite(value) || value < 0) return '—'
  if (value < 60) return `${value} 秒`
  if (value < 3600) return `${Math.floor(value / 60)} 分钟`
  return `${Math.floor(value / 3600)} 小时 ${Math.floor((value % 3600) / 60)} 分钟`
}

function goTask(taskId) {
  proxy.$router.push('/review/task-detail/index/' + taskId)
}

function goRecord(taskId) {
  if (!taskId) return
  proxy.$router.push('/review/record-detail/index/' + taskId)
}

function goDelivery(deliveryId) {
  proxy.$router.push({ path: '/notify/delivery', query: { deliveryId: String(deliveryId) } })
}

onMounted(() => loadAll())
</script>

<style scoped>
.runtime-page { color: #334155; }
.mb16 { margin-bottom: 16px; }
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.updated { font-size: 13px; color: #64748b; }
.runtime-state {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 0;
  color: #64748b;
}
.surface-panel {
  background: #f8faf9;
  border: 1px solid #e2e8e4;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}
.panel-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.panel-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #166534;
}
.hint { font-size: 12px; color: #64748b; }
.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
}
.metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 52px;
}
.metric.wide { grid-column: 1 / -1; }
.metric .label { font-size: 12px; color: #64748b; }
.metric .value { font-size: 18px; font-weight: 600; color: #0f172a; }
.terminal-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.terminal-chip {
  font-size: 12px;
  color: #166534;
  background: #eaf7ee;
  border: 1px solid #bce7c8;
  border-radius: 4px;
  padding: 2px 8px;
}
</style>
