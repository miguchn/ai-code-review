<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
      <el-form-item label="尝试时间">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="-" start-placeholder="开始日期"
          end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
      </el-form-item>
      <el-form-item label="所属项目" prop="projectId">
        <el-select v-model="queryParams.projectId" clearable filterable placeholder="请选择项目" style="width: 200px">
          <el-option v-for="item in projectOptions" :key="item.projectId" :label="item.projectName" :value="item.projectId" />
        </el-select>
      </el-form-item>
      <el-form-item label="投递渠道" prop="channel">
        <el-select v-model="queryParams.channel" clearable placeholder="请选择渠道" style="width: 160px">
          <el-option v-for="dict in review_delivery_channel" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="投递状态" prop="deliveryStatus">
        <el-select v-model="queryParams.deliveryStatus" clearable placeholder="请选择状态" style="width: 130px">
          <el-option v-for="dict in review_delivery_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="PR 编号" prop="prNumber">
        <el-input v-model="queryParams.prNumber" placeholder="请输入 PR 编号" clearable style="width: 130px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="deliveryList" empty-text="暂无投递记录">
      <el-table-column label="最后尝试时间" width="170">
        <template #default="scope">{{ formatDateTime(scope.row.lastAttemptTime) }}</template>
      </el-table-column>
      <el-table-column label="项目名称" prop="projectName" min-width="150" :show-overflow-tooltip="true" />
      <el-table-column label="投递渠道" width="140">
        <template #default="scope">
          <dict-tag :options="review_delivery_channel" :value="scope.row.channel" />
        </template>
      </el-table-column>
      <el-table-column label="触发来源" width="110">
        <template #default="scope">
          <dict-tag v-if="scope.row.triggerSource" :options="review_delivery_trigger_source" :value="scope.row.triggerSource" />
          <span v-else class="empty-tip">—</span>
        </template>
      </el-table-column>
      <el-table-column label="投递状态" width="100">
        <template #default="scope">
          <dict-tag :options="review_delivery_status" :value="scope.row.deliveryStatus" />
        </template>
      </el-table-column>
      <el-table-column label="PR" width="90">
        <template #default="scope">#{{ scope.row.prNumber }}</template>
      </el-table-column>
      <el-table-column label="任务 ID" prop="taskId" width="100" />
      <el-table-column label="失败原因" prop="failureMessage" min-width="220" :show-overflow-tooltip="true" />
      <el-table-column label="尝试次数" prop="attemptCount" width="90" />
      <el-table-column label="操作" width="100" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button v-if="scope.row.deliveryStatus === 'FAILED'" link type="primary"
            :loading="retryingId === scope.row.deliveryId"
            @click="handleRetry(scope.row)" v-hasPermi="['review:delivery:retry']">补发</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="ReviewDeliveryRecord">
import { useRoute } from 'vue-router'
import { listDelivery, retryDeliveryById } from '@/api/review/delivery'
import { listReviewProject } from '@/api/review/project'
import { formatDateTime } from '@/utils/reviewDisplay'

const route = useRoute()

const { proxy } = getCurrentInstance()
const { review_delivery_channel, review_delivery_status, review_delivery_trigger_source } = proxy.useDict(
  'review_delivery_channel',
  'review_delivery_status',
  'review_delivery_trigger_source'
)
const deliveryList = ref([])
const projectOptions = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dateRange = ref([])
const retryingId = ref()

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  projectId: undefined,
  channel: undefined,
  deliveryStatus: undefined,
  prNumber: undefined,
  taskId: undefined
})

function getList() {
  loading.value = true
  const params = proxy.addDateRange({ ...queryParams.value }, dateRange.value)
  listDelivery(params).then(response => {
    deliveryList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => { loading.value = false })
}

function loadProjects() {
  listReviewProject({ pageNum: 1, pageSize: 200, status: '0' }).then(response => {
    projectOptions.value = response.rows || []
  })
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); dateRange.value = []; handleQuery() }

function handleRetry(row) {
  proxy.$modal.confirm('确认向原渠道补发？群内可能出现重复消息').then(() => {
    retryingId.value = row.deliveryId
    return retryDeliveryById(row.deliveryId)
  }).then(() => {
    proxy.$modal.msgSuccess('补发已完成')
    getList()
  }).catch(() => {}).finally(() => { retryingId.value = undefined })
}

loadProjects()
// keep-alive 下从工作台卡片重入 tab 不会重跑 onMounted，激活时回填筛选并刷新
onActivated(() => {
  applyRouteQuery()
  getList()
})

function applyRouteQuery() {
  const q = route.query || {}
  if (q.taskId) {
    queryParams.value.taskId = Number(q.taskId) || q.taskId
  }
  if (q.deliveryStatus) {
    queryParams.value.deliveryStatus = String(q.deliveryStatus)
  }
  if (q.projectId) queryParams.value.projectId = Number(q.projectId) || q.projectId
  if (q.channel) queryParams.value.channel = String(q.channel)
  if (q.prNumber) queryParams.value.prNumber = Number(q.prNumber) || q.prNumber
}
</script>

<style scoped>
.empty-tip { color: var(--el-text-color-placeholder); }
</style>
