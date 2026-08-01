<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
      <el-form-item label="模板名称" prop="templateName">
        <el-input v-model="queryParams.templateName" placeholder="请输入名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="编码" prop="templateCode">
        <el-input v-model="queryParams.templateCode" placeholder="请输入编码" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="技术栈" prop="techStack">
        <el-select v-model="queryParams.techStack" placeholder="请选择" clearable style="width: 140px">
          <el-option v-for="dict in review_tech_stack" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 120px">
          <el-option label="启用" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['review:template:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="templateList" empty-text="暂无审查模板">
      <el-table-column label="名称" prop="templateName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="编码" prop="templateCode" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="技术栈" width="110">
        <template #default="scope">
          <dict-tag :options="review_tech_stack" :value="scope.row.techStack" />
        </template>
      </el-table-column>
      <el-table-column label="类型" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.builtinFlag === '1' ? 'warning' : 'info'" size="small">
            {{ scope.row.builtinFlag === '1' ? '内置' : '自定义' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="版本" prop="versionNo" width="70" />
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="small">
            {{ scope.row.status === '0' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="170">
        <template #default="scope">{{ formatDateTime(scope.row.updateTime || scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="handleView(scope.row)" v-hasPermi="['review:template:query']">查看</el-button>
          <el-button link type="primary" @click="handleCopy(scope.row)" v-hasPermi="['review:template:add']">复制</el-button>
          <el-button v-if="scope.row.builtinFlag !== '1'" link type="primary" @click="handleUpdate(scope.row)"
            v-hasPermi="['review:template:edit']">修改</el-button>
          <el-button v-if="scope.row.builtinFlag !== '1'" link type="primary" @click="handleDelete(scope.row)"
            v-hasPermi="['review:template:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="820px" append-to-body destroy-on-close>
      <el-alert v-if="form.builtinFlag === '1' && !copyMode" class="mb12" type="warning" :closable="false" show-icon
        title="内置模板仅可查看。如需调整，请使用「复制」生成自定义模板后再修改。" />

      <el-collapse v-model="platformRulesActive" class="platform-rules" v-loading="platformRulesLoading">
        <el-collapse-item name="rules">
          <template #title>
            <div class="platform-rules__header">
              <span class="platform-rules__title">{{ platformRules.title || '平台统一审查规则' }}</span>
              <el-tag size="small" type="info" effect="plain">只读 · 协议 v{{ platformRules.protocolVersion || '—' }}</el-tag>
            </div>
          </template>
          <p v-if="platformRules.uiHint" class="platform-rules__hint">{{ platformRules.uiHint }}</p>
          <ul v-if="platformRules.dimensions?.length" class="platform-rules__list">
            <li v-for="dim in platformRules.dimensions" :key="dim.code" class="platform-rules__item">
              <div class="platform-rules__item-head">
                <span class="platform-rules__name">{{ dim.name }}</span>
                <span class="platform-rules__score">{{ dim.maxScore }} 分</span>
              </div>
              <p class="platform-rules__desc">{{ dim.description }}</p>
            </li>
          </ul>
          <div v-if="platformRules.topIssuesHint" class="platform-rules__top">
            {{ platformRules.topIssuesHint }}
          </div>
        </el-collapse-item>
      </el-collapse>

      <el-form ref="templateRef" :model="form" :rules="rules" label-width="100px" :disabled="viewOnly">
        <el-form-item label="名称" prop="templateName">
          <el-input v-model="form.templateName" maxlength="100" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="编码" prop="templateCode">
          <el-input v-model="form.templateCode" maxlength="64" placeholder="英文/下划线，唯一"
            :disabled="!!form.templateId && form.builtinFlag === '1' && !copyMode" />
        </el-form-item>
        <el-form-item label="技术栈" prop="techStack">
          <el-select v-model="form.techStack" style="width: 100%" placeholder="请选择适用技术栈"
            :disabled="form.builtinFlag === '1' && !copyMode">
            <el-option v-for="dict in review_tech_stack" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!copyMode && form.templateId" label="版本">
          <span>v{{ form.versionNo || 1 }}</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="12"
            placeholder="描述本技术栈的专项审查重点；支持占位符 {{pr_title}} {{pr_description}} {{commit_messages}} {{source_branch}} {{target_branch}} {{base_sha}} {{head_sha}} {{diff}}。无需编写评分规则或输出格式。" />
          <div class="inline-tip">只需编写技术栈审查要求；上方公共规则由平台执行时自动追加。任务建单时会快照模板正文与版本，后续修改只影响新任务。</div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="!viewOnly" type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
        <el-button v-if="viewOnly && form.builtinFlag === '1'" type="primary" @click="handleCopy(form)"
          v-hasPermi="['review:template:add']">复制为自定义</el-button>
        <el-button @click="open = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ReviewTemplate">
import {
  listReviewTemplate, getPlatformRules, getReviewTemplate, addReviewTemplate, updateReviewTemplate,
  delReviewTemplate
} from '@/api/review/template'

const { proxy } = getCurrentInstance()
const { review_tech_stack } = proxy.useDict('review_tech_stack')

const templateList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')
const submitting = ref(false)
const viewOnly = ref(false)
const copyMode = ref(false)
const sourceTemplateId = ref()
const platformRulesLoading = ref(false)
const platformRulesActive = ref([])
const platformRules = ref({
  title: '平台统一审查规则',
  protocolVersion: '',
  uiHint: '',
  dimensions: [],
  topIssuesHint: ''
})

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1, pageSize: 10,
    templateName: undefined, templateCode: undefined, techStack: undefined, status: undefined
  },
  rules: {
    templateName: [{ required: true, message: '模板名称不能为空', trigger: 'blur' }],
    templateCode: [{ required: true, message: '模板编码不能为空', trigger: 'blur' }],
    techStack: [{ required: true, message: '请选择技术栈', trigger: 'change' }],
    content: [{ required: true, message: '模板正文不能为空', trigger: 'blur' }],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listReviewTemplate(queryParams.value).then(response => {
    templateList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => { loading.value = false })
}

function reset() {
  form.value = {
    templateId: undefined, templateName: undefined, templateCode: undefined,
    techStack: 'FULLSTACK', content: '', versionNo: 1, builtinFlag: '0',
    status: '0', remark: undefined
  }
  viewOnly.value = false
  copyMode.value = false
  sourceTemplateId.value = undefined
  proxy.resetForm('templateRef')
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }

function loadPlatformRules() {
  if (platformRules.value.dimensions?.length) {
    return Promise.resolve()
  }
  platformRulesLoading.value = true
  return getPlatformRules().then(response => {
    platformRules.value = response.data || platformRules.value
  }).catch(() => {
    // 规则加载失败不阻断模板编辑；保留空态提示
  }).finally(() => {
    platformRulesLoading.value = false
  })
}

function openDialog(dialogTitle) {
  platformRulesActive.value = []
  open.value = true
  title.value = dialogTitle
  loadPlatformRules()
}

function handleAdd() {
  reset()
  openDialog('新增审查模板')
}

function handleView(row) {
  reset()
  getReviewTemplate(row.templateId).then(response => {
    form.value = response.data || {}
    viewOnly.value = true
    openDialog('查看审查模板')
  })
}

function handleUpdate(row) {
  if (row.builtinFlag === '1') {
    proxy.$modal.msgWarning('内置模板不可直接修改，请先复制为自定义模板')
    return
  }
  reset()
  getReviewTemplate(row.templateId).then(response => {
    form.value = response.data || {}
    openDialog('修改审查模板')
  })
}

function handleCopy(row) {
  const id = row.templateId
  getReviewTemplate(id).then(response => {
    const source = response.data || {}
    reset()
    copyMode.value = true
    sourceTemplateId.value = id
    form.value = {
      templateId: undefined,
      templateName: (source.templateName || '') + '（副本）',
      templateCode: (source.templateCode || 'template') + '_copy',
      techStack: source.techStack || 'FULLSTACK',
      content: source.content || '',
      versionNo: 1,
      builtinFlag: '0',
      status: '0',
      remark: source.remark
    }
    openDialog('复制审查模板')
  })
}

function submitForm() {
  proxy.$refs.templateRef.validate(valid => {
    if (!valid) return
    submitting.value = true
    // 复制弹窗允许改正文后再保存：走新增自定义，避免 copy 接口忽略正文
    const action = (!copyMode.value && form.value.templateId)
      ? updateReviewTemplate(form.value)
      : addReviewTemplate({ ...form.value, templateId: undefined, builtinFlag: '0', versionNo: 1 })
    action.then(() => {
      proxy.$modal.msgSuccess(copyMode.value ? '复制成功' : (form.value.templateId ? '修改成功' : '新增成功'))
      open.value = false
      getList()
    }).finally(() => { submitting.value = false })
  })
}

function handleDelete(row) {
  if (row.builtinFlag === '1') {
    proxy.$modal.msgWarning('内置模板不能删除')
    return
  }
  proxy.$modal.confirm('是否确认删除模板“' + row.templateName + '”？').then(() => {
    return delReviewTemplate(row.templateId)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function formatDateTime(value) {
  return value ? proxy.parseTime(value) : '—'
}

getList()
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.inline-tip { margin-top: 6px; color: var(--el-text-color-secondary); font-size: 12px; line-height: 20px; }

.platform-rules {
  margin-bottom: 16px;
  border: 1px solid #D7E3DA;
  border-radius: 8px;
  background: #F4F8F5;
  --el-collapse-header-height: 44px;
  --el-collapse-header-bg-color: transparent;
  --el-collapse-content-bg-color: transparent;
  --el-collapse-border-color: transparent;
}
.platform-rules :deep(.el-collapse-item__header) {
  padding: 0 16px;
  border-bottom: 0;
  line-height: 1.4;
  color: inherit;
}
.platform-rules :deep(.el-collapse-item__wrap) {
  border-bottom: 0;
}
.platform-rules :deep(.el-collapse-item__content) {
  padding: 0 16px 14px;
}
.platform-rules__header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-right: 8px;
}
.platform-rules__title {
  font-size: 14px;
  font-weight: 600;
  color: #166534;
}
.platform-rules__hint {
  margin: 0 0 12px;
  font-size: 12px;
  line-height: 20px;
  color: #475569;
}
.platform-rules__list {
  margin: 0;
  padding: 0;
  list-style: none;
}
.platform-rules__item + .platform-rules__item {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #DCE8DF;
}
.platform-rules__item-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}
.platform-rules__name {
  font-size: 13px;
  font-weight: 600;
  color: #1F2937;
}
.platform-rules__score {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  color: #15803D;
}
.platform-rules__desc {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 18px;
  color: #64748B;
}
.platform-rules__top {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #DCE8DF;
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
  color: #334155;
}
</style>
