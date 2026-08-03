<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="92px">
      <el-form-item label="所属项目" prop="projectId">
        <el-select v-model="queryParams.projectId" clearable filterable placeholder="请选择项目" style="width: 220px">
          <el-option v-for="item in projectOptions" :key="item.projectId" :label="item.projectName" :value="item.projectId" />
        </el-select>
      </el-form-item>
      <el-form-item label="PR 编号" prop="prNumber">
        <el-input v-model="queryParams.prNumber" placeholder="请输入 PR 编号" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="PR 发起人" prop="prAuthor">
        <el-input v-model="queryParams.prAuthor" placeholder="GitHub login" clearable style="width: 150px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="审查结论" prop="reviewConclusion">
        <el-select v-model="queryParams.reviewConclusion" clearable placeholder="请选择结论" style="width: 140px">
          <el-option v-for="item in conclusionOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="完成时间">
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

    <el-table v-loading="loading" :data="recordList" empty-text="暂无审查记录">
      <el-table-column label="项目名称" min-width="160">
        <template #default="scope">
          <div class="project-cell">
            <span class="project-name" :title="scope.row.projectName || ''">{{ emptyDash(scope.row.projectName) }}</span>
            <span class="system-name" :title="scope.row.businessSystemName || ''">{{ emptyDash(scope.row.businessSystemName) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="PR 信息" min-width="220">
        <template #default="scope">
          <a v-if="buildGithubPrUrl(scope.row)" class="pr-link" :href="buildGithubPrUrl(scope.row)" target="_blank" rel="noopener noreferrer"
            :title="'打开 GitHub PR #' + scope.row.prNumber">
            <el-tag size="small" type="primary">#{{ scope.row.prNumber }}</el-tag>
            <span class="pr-title">{{ emptyDash(scope.row.prTitle) }}</span>
          </a>
          <div v-else class="pr-cell">
            <el-tag size="small" type="primary">#{{ scope.row.prNumber }}</el-tag>
            <span class="pr-title">{{ emptyDash(scope.row.prTitle) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="PR 发起人" width="120" :show-overflow-tooltip="true">
        <template #default="scope">{{ emptyDash(scope.row.prAuthor) }}</template>
      </el-table-column>
      <el-table-column label="分支" min-width="170">
        <template #default="scope">
          <span class="branch-flow">{{ emptyDash(scope.row.sourceBranch) }} → {{ emptyDash(scope.row.targetBranch) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="代码变更" width="150" align="center">
        <template #default="scope">
          <span class="diff-lines">{{ formatCodeChange(scope.row.changedFiles, scope.row.additions, scope.row.deletions) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审查结论" width="110">
        <template #default="scope">
          <el-tag :type="recordConclusionTagType(scope.row)" size="small">{{ recordConclusionLabel(scope.row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="评分" width="80" align="center">
        <template #default="scope">
          {{ scope.row.taskStatus === 'FAILED' ? '--' : formatScore(scope.row.totalScore) }}
        </template>
      </el-table-column>
      <el-table-column label="重点问题" min-width="180">
        <template #default="scope">
          <span :title="scope.row.taskStatus === 'FAILED' ? '' : '基于结构化 Top 3 重点问题分级统计，非全量问题'">
            {{ formatFocusIssueCounts(scope.row) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="完成时间" width="170">
        <template #default="scope">{{ formatDateTime(scope.row.finishedTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" v-hasPermi="['review:record:query']" @click="handleDetail(scope.row)">查看详情</el-button>
          <el-button link type="primary" v-hasPermi="['review:record:query']" @click="handleViewIssues(scope.row)">查看问题</el-button>
          <el-button link type="primary" :disabled="!buildGithubPrUrl(scope.row)" @click="openPr(scope.row)">打开 PR</el-button>
          <el-button
            v-if="scope.row.taskStatus === 'FAILED'"
            link type="primary"
            v-hasPermi="['review:task:retry']"
            @click="handleRetry(scope.row)"
          >重新执行</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="ReviewRecord">
import { listReviewRecord } from '@/api/review/record'
import { listReviewProject } from '@/api/review/project'
import { retryReviewTask } from '@/api/review/task'
import {
  emptyDash, formatScore, formatCodeChange, formatFocusIssueCounts,
  recordConclusionLabel, recordConclusionTagType, buildGithubPrUrl, formatDateTime
} from '@/utils/reviewDisplay'

const route = useRoute()
const { proxy } = getCurrentInstance()

const conclusionOptions = [
  { label: '通过', value: 'PASS' },
  { label: '建议修改', value: 'WARN' },
  { label: '高风险', value: 'BLOCK' },
  { label: '执行失败', value: 'FAILED' }
]

const recordList = ref([])
const projectOptions = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dateRange = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectId: undefined,
    prNumber: undefined,
    prAuthor: undefined,
    reviewConclusion: undefined
  }
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listReviewRecord(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    recordList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => { loading.value = false })
}

function loadProjects() {
  listReviewProject({ pageNum: 1, pageSize: 100 }).then(response => {
    projectOptions.value = response.rows || []
  })
}

function handleDetail(row) {
  proxy.$router.push('/review/record-detail/index/' + row.taskId)
}

function handleViewIssues(row) {
  proxy.$router.push({ path: '/review/record-detail/index/' + row.taskId, query: { focus: 'issues' } })
}

function openPr(row) {
  const url = buildGithubPrUrl(row)
  if (!url) {
    proxy.$modal.msgWarning('暂无法生成 GitHub PR 链接')
    return
  }
  window.open(url, '_blank', 'noopener,noreferrer')
}

function handleRetry(row) {
  proxy.$modal.confirm('确认重新执行该失败任务？历史执行记录将保留。').then(() => {
    return retryReviewTask(row.taskId)
  }).then(() => {
    proxy.$modal.msgSuccess('已提交重新执行')
    getList()
  }).catch(() => {})
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); dateRange.value = []; handleQuery() }

function applyRouteQuery() {
  const q = route.query || {}
  if (q.reviewConclusion) queryParams.value.reviewConclusion = String(q.reviewConclusion)
  if (q.projectId) queryParams.value.projectId = Number(q.projectId) || q.projectId
  if (q.prNumber) queryParams.value.prNumber = Number(q.prNumber) || q.prNumber
  if (q.prAuthor) queryParams.value.prAuthor = String(q.prAuthor)
  if (q.beginTime || q.endTime) {
    dateRange.value = [q.beginTime ? String(q.beginTime) : '', q.endTime ? String(q.endTime) : '']
  }
}

loadProjects()
// keep-alive 下从工作台卡片重入 tab 不会重跑 onMounted，激活时回填筛选并刷新
onActivated(() => {
  applyRouteQuery()
  getList()
})
</script>

<style scoped>
.project-cell { display: flex; flex-direction: column; gap: 2px; line-height: 1.4; }
.project-name { font-weight: 500; color: var(--el-text-color-primary); }
.system-name { font-size: 12px; color: var(--el-text-color-secondary); }
.pr-cell, .pr-link {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.pr-link {
  color: var(--el-color-primary);
  text-decoration: none;
}
.pr-link:hover .pr-title { text-decoration: underline; }
.pr-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.branch-flow { color: var(--el-text-color-regular); }
.diff-lines {
  font-variant-numeric: tabular-nums;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}
</style>
