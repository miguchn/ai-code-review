<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
      <el-form-item label="所属项目" prop="projectId">
        <el-select v-model="queryParams.projectId" clearable filterable placeholder="请选择项目" style="width: 220px">
          <el-option v-for="item in projectOptions" :key="item.projectId" :label="item.projectName" :value="item.projectId" />
        </el-select>
      </el-form-item>
      <el-form-item label="PR 编号" prop="prNumber">
        <el-input v-model="queryParams.prNumber" placeholder="请输入 PR 编号" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="任务状态" prop="taskStatus">
        <el-select v-model="queryParams.taskStatus" clearable placeholder="请选择状态" style="width: 130px">
          <el-option v-for="dict in review_task_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="触发时间">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="-" start-placeholder="开始日期"
          end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="taskList" empty-text="暂无审查任务">
      <el-table-column label="任务 ID" prop="taskId" width="90" />
      <el-table-column label="所属项目" prop="projectName" min-width="150" :show-overflow-tooltip="true" />
      <el-table-column label="Pull Request" min-width="220">
        <template #default="scope">
          <div class="pr-cell">
            <el-tag size="small" type="primary">#{{ scope.row.prNumber }}</el-tag>
            <span class="pr-title" :title="scope.row.prTitle">{{ scope.row.prTitle }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="分支" min-width="200">
        <template #default="scope">
          <span class="branch-flow">{{ scope.row.sourceBranch }} → {{ scope.row.targetBranch }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Head SHA" width="110">
        <template #default="scope"><code class="sha">{{ shortSha(scope.row.headSha) }}</code></template>
      </el-table-column>
      <el-table-column label="任务状态" width="100">
        <template #default="scope">
          <dict-tag :options="review_task_status" :value="scope.row.taskStatus" />
        </template>
      </el-table-column>
      <el-table-column label="触发方式" width="110">
        <template #default="scope">
          <dict-tag :options="review_trigger_type" :value="scope.row.triggerType" />
        </template>
      </el-table-column>
      <el-table-column label="触发时间" prop="createTime" width="165" />
      <el-table-column label="失败原因" min-width="150">
        <template #default="scope">
          <span v-if="scope.row.failureMessage" class="failure-message" :title="scope.row.failureMessage">{{ scope.row.failureMessage }}</span>
          <span v-else class="empty-tip">—</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="ReviewTask">
import { listReviewTask } from '@/api/review/task'
import { listReviewProject } from '@/api/review/project'

const { proxy } = getCurrentInstance()
const { review_task_status, review_trigger_type } = proxy.useDict('review_task_status', 'review_trigger_type')

const taskList = ref([])
const projectOptions = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dateRange = ref([])

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10, projectId: undefined, prNumber: undefined, taskStatus: undefined }
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listReviewTask(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    taskList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => { loading.value = false })
}

function loadProjects() {
  listReviewProject({ pageNum: 1, pageSize: 100 }).then(response => {
    projectOptions.value = response.rows || []
  })
}

function shortSha(sha) {
  return sha ? sha.substring(0, 7) : '—'
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); dateRange.value = []; handleQuery() }

loadProjects()
getList()
</script>

<style scoped>
.pr-cell { display: flex; align-items: center; gap: 8px; }
.pr-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.branch-flow { font-family: var(--el-font-family); color: var(--el-text-color-regular); }
.sha { font-size: 12px; color: var(--el-text-color-secondary); }
.failure-message { color: var(--el-color-danger); }
.empty-tip { color: var(--el-text-color-placeholder); }
</style>
