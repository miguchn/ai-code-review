<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
      <el-form-item label="凭据名称" prop="credentialName">
        <el-input v-model="queryParams.credentialName" placeholder="请输入凭据名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="Git 平台" prop="provider">
        <el-select v-model="queryParams.provider" clearable placeholder="请选择平台" style="width: 150px">
          <el-option v-for="item in gitProviderOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="请选择状态" style="width: 120px">
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['review:credential:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="credentialList" empty-text="暂无访问凭据">
      <el-table-column label="凭据名称" prop="credentialName" min-width="170" :show-overflow-tooltip="true" />
      <el-table-column label="Git 平台" width="130">
        <template #default="scope">
          <dict-tag :options="gitProviderOptions" :value="scope.row.provider" />
        </template>
      </el-table-column>
      <el-table-column label="服务地址" min-width="180" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.serverUrl || '—' }}</template>
      </el-table-column>
      <el-table-column label="认证方式" prop="authType" width="110" />
      <el-table-column label="Token" width="100">
        <template #default="scope"><el-tag :type="scope.row.tokenConfigured ? 'success' : 'danger'" size="small">{{ scope.row.tokenConfigured ? '已配置' : '未配置' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="使用状态" width="130">
        <template #default="scope">
          <el-tag :type="scope.row.referenceCount > 0 ? 'warning' : 'info'" size="small">
            {{ scope.row.referenceCount > 0 ? '已被 ' + scope.row.referenceCount + ' 个项目引用' : '未使用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近检测" min-width="170">
        <template #default="scope">
          <el-tooltip :content="scope.row.lastCheckMessage || '尚未检测'" placement="top">
            <el-tag :type="checkTagType(scope.row.lastCheckStatus)" size="small">{{ checkStatusText(scope.row.lastCheckStatus) }}</el-tag>
          </el-tooltip>
          <span v-if="scope.row.lastCheckTime" class="check-time">{{ formatDateTime(scope.row.lastCheckTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="scope"><el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="修改时间" width="170">
        <template #default="scope">{{ formatDateTime(scope.row.updateTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Connection" :loading="testingId === scope.row.credentialId"
            @click="handleTest(scope.row)" v-hasPermi="['review:credential:test']">检测</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['review:credential:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['review:credential:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="credentialRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="凭据名称" prop="credentialName">
          <el-input v-model="form.credentialName" placeholder="如：GitLab 试点仓库只读凭据" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Git 平台" prop="provider">
              <el-select v-model="form.provider" placeholder="请选择平台" :disabled="!!form.credentialId" @change="handleProviderChange">
                <el-option v-for="item in gitProviderOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="认证方式"><el-input model-value="Personal Access Token" disabled /></el-form-item></el-col>
        </el-row>
        <el-form-item v-if="requiresServerUrl(form.provider)" label="服务地址" prop="serverUrl">
          <el-input v-model="form.serverUrl" placeholder="https://gitlab.example.com" />
        </el-form-item>
        <el-form-item label="Token" prop="token">
          <el-input v-model="form.token" type="password" show-password autocomplete="new-password"
            :placeholder="tokenPlaceholder(form.provider, !!form.credentialId)" />
        </el-form-item>
        <el-alert title="Token 只在提交时发送，编辑页面不会回显历史值。" type="info" :closable="false" show-icon class="form-tip" />
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="可选，例如凭据用途和最小权限范围" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ReviewCredential">
import { listGitCredential, getGitCredential, addGitCredential, updateGitCredential, delGitCredential, testGitCredential } from '@/api/review/credential'

const { proxy } = getCurrentInstance()
const { review_git_provider } = proxy.useDict('review_git_provider')

const FALLBACK_GIT_PROVIDERS = [
  { label: 'GitHub', value: 'GITHUB' },
  { label: 'GitLab', value: 'GITLAB' },
  { label: 'Gitee（码云）', value: 'GITEE' },
  { label: 'Gitea', value: 'GITEA' }
]

const gitProviderOptions = computed(() => {
  const dictOptions = review_git_provider.value || []
  return dictOptions.length ? dictOptions : FALLBACK_GIT_PROVIDERS
})

const credentialList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')
const testingId = ref()

const validateToken = (rule, value, callback) => {
  if (!form.value.credentialId && !value) callback(new Error('新增凭据时必须输入 Token'))
  else callback()
}

const validateServerUrl = (rule, value, callback) => {
  if (!requiresServerUrl(form.value.provider)) {
    callback()
    return
  }
  if (!value || !String(value).trim()) {
    callback(new Error('GitLab / Gitea 必须填写服务地址'))
    return
  }
  callback()
}

const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, credentialName: undefined, provider: undefined, status: undefined },
  rules: {
    credentialName: [{ required: true, message: '凭据名称不能为空', trigger: 'blur' }],
    provider: [{ required: true, message: '请选择 Git 平台', trigger: 'change' }],
    serverUrl: [{ validator: validateServerUrl, trigger: 'blur' }],
    token: [{ validator: validateToken, trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function requiresServerUrl(provider) {
  const code = (provider || '').toUpperCase()
  return code === 'GITLAB' || code === 'GITEA'
}

function tokenPlaceholder(provider, isEdit) {
  if (isEdit) return '留空保留原 Token；修改时请重新输入'
  const code = (provider || 'GITHUB').toUpperCase()
  if (code === 'GITLAB') return '请输入 GitLab Personal Access Token'
  if (code === 'GITEE') return '请输入 Gitee 私人令牌'
  if (code === 'GITEA') return '请输入 Gitea Access Token'
  return '请输入 GitHub Personal Access Token'
}

function providerLabel(provider) {
  const hit = gitProviderOptions.value.find(item => item.value === provider)
  return hit?.label || provider || 'Git'
}

function handleProviderChange() {
  if (!requiresServerUrl(form.value.provider)) {
    form.value.serverUrl = undefined
  }
  proxy.$refs.credentialRef?.clearValidate(['serverUrl'])
}

function getList() {
  loading.value = true
  listGitCredential(queryParams.value).then(response => {
    credentialList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => { loading.value = false })
}

function reset() {
  form.value = {
    credentialId: undefined,
    credentialName: undefined,
    provider: 'GITHUB',
    authType: 'PAT',
    serverUrl: undefined,
    token: undefined,
    status: '0',
    remark: undefined
  }
  proxy.resetForm('credentialRef')
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function cancel() { open.value = false; reset() }
function handleAdd() { reset(); open.value = true; title.value = '新增 Git 访问凭据' }

function handleUpdate(row) {
  reset()
  getGitCredential(row.credentialId).then(response => {
    form.value = { ...response.data, token: undefined }
    open.value = true
    title.value = '修改 ' + providerLabel(form.value.provider) + ' 访问凭据'
  })
}

function submitForm() {
  proxy.$refs.credentialRef.validate(valid => {
    if (!valid) return
    const payload = { ...form.value }
    if (!requiresServerUrl(payload.provider)) {
      payload.serverUrl = null
    }
    const action = payload.credentialId ? updateGitCredential(payload) : addGitCredential(payload)
    action.then(() => {
      proxy.$modal.msgSuccess(payload.credentialId ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除凭据“' + row.credentialName + '”？').then(() => delGitCredential(row.credentialId)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function handleTest(row) {
  testingId.value = row.credentialId
  testGitCredential(row.credentialId).then(response => {
    const result = response.data
    if (result.success) proxy.$modal.msgSuccess(result.message)
    else proxy.$modal.msgError(result.message)
    getList()
  }).finally(() => { testingId.value = undefined })
}

function checkStatusText(status) { return { SUCCESS: '连接正常', FAILED: '连接失败', UNTESTED: '未检测' }[status] || '未检测' }
function checkTagType(status) { return { SUCCESS: 'success', FAILED: 'danger', UNTESTED: 'info' }[status] || 'info' }

getList()
</script>

<style scoped>
.check-time { margin-left: 8px; color: var(--el-text-color-secondary); font-size: 12px; }
.form-tip {
  margin: -2px 0 12px 100px;
  padding: 0;
  background: transparent;
  line-height: 20px;
}
.form-tip :deep(.el-alert__icon) { margin-right: 6px; color: var(--el-color-info); }
.form-tip :deep(.el-alert__content) { padding: 0; }
.form-tip :deep(.el-alert__title) {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 400;
  line-height: 20px;
}
</style>
