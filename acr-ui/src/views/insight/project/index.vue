<template>
  <div class="app-container insight-page">
    <el-form :inline="true" :model="query" class="insight-filters">
      <el-form-item label="时间范围">
        <el-radio-group v-model="rangePreset" @change="onPresetChange">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
          <el-radio-button value="custom">自定义</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="rangePreset === 'custom'" label="自定义">
        <el-date-picker
          v-model="customRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始"
          end-placeholder="结束"
          :clearable="false"
        />
      </el-form-item>
      <el-form-item label="业务系统">
        <el-select v-model="query.businessSystemId" clearable filterable placeholder="全部" style="width: 180px">
          <el-option v-for="item in businessSystems" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="项目">
        <el-select v-model="query.projectId" clearable filterable placeholder="全部" style="width: 200px">
          <el-option v-for="item in projectOptions" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadData">查询</el-button>
      </el-form-item>
    </el-form>

    <div v-if="loading">
      <el-skeleton :rows="6" animated />
    </div>
    <div v-else-if="error" class="insight-state">
      <span>项目矩阵加载失败</span>
      <el-button type="primary" link @click="loadData">重试</el-button>
    </div>
    <template v-else>
      <el-alert
        v-if="!rows.length"
        type="info"
        :closable="false"
        show-icon
        class="mb16"
        title="暂无可见项目或选定范围内无审查数据"
        description="请确认数据权限范围内已接入代码项目，并完成聚合刷新。数据积累起始日期见详情页空态提示。"
      />
      <el-table :data="rows" @sort-change="onSortChange" empty-text="暂无项目数据">
        <el-table-column label="项目" prop="projectName" min-width="160" :show-overflow-tooltip="true">
          <template #default="scope">
            <el-link type="primary" @click="goDetail(scope.row)">{{ scope.row.projectName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="业务系统" prop="businessSystemName" min-width="120" :show-overflow-tooltip="true" />
        <el-table-column label="负责人" prop="ownerName" width="100" :show-overflow-tooltip="true" />
        <el-table-column label="任务数" prop="taskTotal" width="100" sortable="custom" />
        <el-table-column label="成功率" prop="successRate" width="110" sortable="custom">
          <template #default="scope">{{ formatRatio(scope.row.successRate) }}</template>
        </el-table-column>
        <el-table-column label="新增问题" prop="issueNew" width="110" sortable="custom" />
        <el-table-column label="未关闭重点" prop="openFocusIssues" width="120" sortable="custom" />
        <el-table-column label="处置率" prop="dispositionRate" width="110" sortable="custom">
          <template #default="scope">{{ formatRatio(scope.row.dispositionRate) }}</template>
        </el-table-column>
        <el-table-column label="最近审查时间" prop="lastReviewTime" min-width="170" sortable="custom">
          <template #default="scope">{{ scope.row.lastReviewTime || '--' }}</template>
        </el-table-column>
      </el-table>
    </template>
  </div>
</template>

<script setup name="InsightProject">
import { getReviewProjectOptions } from '@/api/review/project'
import { listInsightProjects } from '@/api/insight'
import { formatRatio, loadInsightFilters, saveInsightFilters, toIdParam, toRangePreset, toDateRangeParam } from '../components/insightFilter'

const router = useRouter()

const loading = ref(false)
const error = ref(false)
const rows = ref([])
const businessSystems = ref([])
const projectOptions = ref([])
const rangePreset = ref(7)
const customRange = ref([])
const orderBy = ref('-taskTotal')
const query = reactive({
  businessSystemId: undefined,
  projectId: undefined
})

function onPresetChange() {
  /* handled in buildParams */
}

function buildParams() {
  const params = {
    businessSystemId: toIdParam(query.businessSystemId),
    projectId: toIdParam(query.projectId),
    orderBy: orderBy.value
  }
  if (rangePreset.value === 'custom' && customRange.value?.length === 2) {
    params.beginDate = customRange.value[0]
    params.endDate = customRange.value[1]
  } else {
    params.days = Number(rangePreset.value) || 7
  }
  return params
}

function onSortChange({ prop, order }) {
  if (!prop || !order) {
    orderBy.value = '-taskTotal'
  } else {
    orderBy.value = (order === 'descending' ? '-' : '') + prop
  }
  loadData()
}

function goDetail(row) {
  const q = {}
  if (rangePreset.value === 'custom' && customRange.value?.length === 2) {
    q.beginDate = customRange.value[0]
    q.endDate = customRange.value[1]
  } else {
    q.days = Number(rangePreset.value) || 7
  }
  router.push({ path: `/insight/project-detail/index/${row.projectId}`, query: q })
}

async function loadOptions() {
  try {
    const res = await getReviewProjectOptions()
    businessSystems.value = res.data?.businessSystems || []
    // 无独立项目下拉源时，用矩阵行回填；此处先留空，loadData 后补齐
    projectOptions.value = []
  } catch (e) {
    businessSystems.value = []
    projectOptions.value = []
  }
}

async function loadData() {
  loading.value = true
  error.value = false
  saveInsightFilters('project', {
    rangePreset: rangePreset.value,
    customRange: customRange.value,
    businessSystemId: query.businessSystemId,
    projectId: query.projectId
  })
  try {
    const res = await listInsightProjects(buildParams())
    rows.value = res.data || []
    if (!projectOptions.value.length) {
      projectOptions.value = rows.value.map(r => ({ id: r.projectId, label: r.projectName }))
    }
  } catch (e) {
    error.value = true
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const remembered = loadInsightFilters('project')
  if (remembered) {
    rangePreset.value = toRangePreset(remembered.rangePreset)
    customRange.value = toDateRangeParam(remembered.customRange)
    query.businessSystemId = toIdParam(remembered.businessSystemId)
    query.projectId = toIdParam(remembered.projectId)
  }
  await loadOptions()
  await loadData()
})
</script>

<style scoped lang="scss">
.insight-state {
  display: flex;
  gap: 12px;
  align-items: center;
  color: var(--text-secondary);
  padding: 40px 0;
}
.mb16 { margin-bottom: 16px; }
</style>
