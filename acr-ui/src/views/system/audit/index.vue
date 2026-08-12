<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="82px">
      <el-form-item label="操作类型" prop="action">
        <el-input v-model="queryParams.action" placeholder="如 ISSUE_CLOSE" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="对象类型" prop="objectType">
        <el-input v-model="queryParams.objectType" placeholder="如 REVIEW_ISSUE" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="操作人员" prop="operator">
        <el-input v-model="queryParams.operator" placeholder="请输入操作人员" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="auditList">
      <el-table-column label="时间" prop="auditTime" width="180" />
      <el-table-column label="操作人员" prop="operator" width="120" show-overflow-tooltip />
      <el-table-column label="操作" prop="action" width="190" show-overflow-tooltip />
      <el-table-column label="对象" prop="objectType" width="150" show-overflow-tooltip />
      <el-table-column label="对象名称" prop="objectName" show-overflow-tooltip />
      <el-table-column label="原因" prop="reason" show-overflow-tooltip />
      <el-table-column label="详情" width="80" align="center">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="showDetail(scope.row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog v-model="detailVisible" title="业务审计详情" width="760px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="事件标识">{{ detail.eventKey }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ detail.source }}</el-descriptions-item>
        <el-descriptions-item label="对象 ID">{{ detail.objectId }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ detail.auditTime }}</el-descriptions-item>
        <el-descriptions-item label="操作原因" :span="2">{{ detail.reason || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-row :gutter="12" class="audit-value-row">
        <el-col :span="12">
          <div class="audit-value-title">前值</div>
          <pre>{{ formatValue(detail.beforeValue) }}</pre>
        </el-col>
        <el-col :span="12">
          <div class="audit-value-title">后值</div>
          <pre>{{ formatValue(detail.afterValue) }}</pre>
        </el-col>
      </el-row>
      <div class="audit-value-title">关联对象</div>
      <pre>{{ formatValue(detail.relatedObject) }}</pre>
    </el-dialog>
  </div>
</template>

<script setup name="BusinessAudit">
import { list } from '@/api/system/businessAudit'

const { proxy } = getCurrentInstance()
const auditList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailVisible = ref(false)
const detail = ref({})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    action: undefined,
    objectType: undefined,
    operator: undefined
  }
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  list(queryParams.value).then(response => {
    auditList.value = response.rows
    total.value = response.total
    loading.value = false
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

function showDetail(row) {
  detail.value = row
  detailVisible.value = true
}

function formatValue(value) {
  if (!value) return '—'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch (e) {
    return value
  }
}

getList()
</script>

<style scoped>
.audit-value-row {
  margin-top: 18px;
}

.audit-value-title {
  margin: 12px 0 6px;
  font-weight: 600;
}

pre {
  max-height: 240px;
  overflow: auto;
  padding: 10px;
  background: var(--el-fill-color-light);
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
