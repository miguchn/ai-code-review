<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
      <el-form-item label="渠道名称" prop="channelName">
        <el-input v-model="queryParams.channelName" placeholder="请输入渠道名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="渠道类型" prop="channelType">
        <el-select v-model="queryParams.channelType" clearable placeholder="请选择类型" style="width: 150px">
          <el-option v-for="dict in review_notify_channel_type" :key="dict.value" :label="dict.label" :value="dict.value" />
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['review:notify:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="channelList" empty-text="暂无通知渠道">
      <el-table-column label="渠道名称" prop="channelName" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="渠道类型" width="120">
        <template #default="scope">
          <dict-tag :options="review_notify_channel_type" :value="scope.row.channelType" />
        </template>
      </el-table-column>
      <el-table-column label="Webhook" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.webhookUrlConfigured ? 'success' : 'danger'" size="small">
            {{ scope.row.webhookUrlConfigured ? '已配置' : '未配置' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="加签 Secret" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.secretConfigured ? 'success' : 'info'" size="small">
            {{ scope.row.secretConfigured ? '已配置' : '未配置' }}
          </el-tag>
        </template>
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
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Promotion" :loading="testingId === scope.row.channelId"
            @click="handleTest(scope.row)" v-hasPermi="['review:notify:test']">测试</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['review:notify:edit']">修改</el-button>
          <el-button link type="primary" @click="handleStatusChange(scope.row)" v-hasPermi="['review:notify:status']">
            {{ scope.row.status === '0' ? '停用' : '启用' }}
          </el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['review:notify:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="620px" append-to-body>
      <el-form ref="channelRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="渠道名称" prop="channelName">
          <el-input v-model="form.channelName" placeholder="如：研发群钉钉机器人" />
        </el-form-item>
        <el-form-item label="渠道类型" prop="channelType">
          <el-select v-model="form.channelType" placeholder="请选择渠道类型" :disabled="!!form.channelId">
            <el-option v-for="dict in review_notify_channel_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="Webhook URL" prop="webhookUrl">
          <el-input v-model="form.webhookUrl" type="password" show-password autocomplete="new-password"
            :placeholder="form.channelId ? '留空保留原 URL；修改时请重新输入' : '请输入群机器人 Webhook URL'" />
        </el-form-item>
        <el-form-item label="加签 Secret" prop="secret">
          <el-input v-model="form.secret" type="password" show-password autocomplete="new-password"
            :placeholder="form.channelId ? '留空保留原 Secret；清空并提交可清除加签' : '可选，钉钉等渠道启用加签时填写'" />
        </el-form-item>
        <el-alert title="Webhook URL 与 Secret 只在提交时发送，编辑页面不会回显历史值。" type="info" :closable="false" show-icon class="form-tip" />
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="可选，例如渠道用途或群名称" />
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

<script setup name="ReviewNotifyChannel">
import {
  listNotifyChannel, getNotifyChannel, addNotifyChannel, updateNotifyChannel,
  delNotifyChannel, changeNotifyChannelStatus, testNotifyChannel
} from '@/api/review/notifyChannel'

const { proxy } = getCurrentInstance()
const { review_notify_channel_type } = proxy.useDict('review_notify_channel_type')
const channelList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')
const testingId = ref()

const validateWebhookUrl = (rule, value, callback) => {
  if (!form.value.channelId && !value) callback(new Error('新增渠道时必须输入 Webhook URL'))
  else callback()
}

const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, channelName: undefined, channelType: undefined, status: undefined },
  rules: {
    channelName: [{ required: true, message: '渠道名称不能为空', trigger: 'blur' }],
    channelType: [{ required: true, message: '请选择渠道类型', trigger: 'change' }],
    webhookUrl: [{ validator: validateWebhookUrl, trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listNotifyChannel(queryParams.value).then(response => {
    channelList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => { loading.value = false })
}

function reset() {
  form.value = {
    channelId: undefined, channelName: undefined, channelType: undefined,
    webhookUrl: undefined, secret: undefined, status: '0', remark: undefined
  }
  proxy.resetForm('channelRef')
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function cancel() { open.value = false; reset() }
function handleAdd() { reset(); open.value = true; title.value = '新增通知渠道' }

function handleUpdate(row) {
  reset()
  getNotifyChannel(row.channelId).then(response => {
    form.value = { ...response.data, webhookUrl: undefined, secret: undefined }
    open.value = true
    title.value = '修改通知渠道'
  })
}

function submitForm() {
  proxy.$refs.channelRef.validate(valid => {
    if (!valid) return
    const action = form.value.channelId ? updateNotifyChannel(form.value) : addNotifyChannel(form.value)
    action.then(() => {
      proxy.$modal.msgSuccess(form.value.channelId ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除渠道“' + row.channelName + '”？').then(() => delNotifyChannel(row.channelId)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function handleStatusChange(row) {
  const targetStatus = row.status === '0' ? '1' : '0'
  const label = targetStatus === '0' ? '启用' : '停用'
  proxy.$modal.confirm('是否确认' + label + '渠道“' + row.channelName + '”？').then(() => {
    return changeNotifyChannelStatus({ channelId: row.channelId, status: targetStatus })
  }).then(() => {
    row.status = targetStatus
    proxy.$modal.msgSuccess('渠道已' + label)
  }).catch(() => {})
}

function handleTest(row) {
  testingId.value = row.channelId
  testNotifyChannel(row.channelId).then(response => {
    const result = response.data
    if (result.success) proxy.$modal.msgSuccess(result.message)
    else proxy.$modal.msgError(result.message)
    getList()
  }).finally(() => { testingId.value = undefined })
}

function checkStatusText(status) { return { SUCCESS: '发送正常', FAILED: '发送失败', UNTESTED: '未检测' }[status] || '未检测' }
function checkTagType(status) { return { SUCCESS: 'success', FAILED: 'danger', UNTESTED: 'info' }[status] || 'info' }

getList()
</script>

<style scoped>
.check-time { margin-left: 8px; color: var(--el-text-color-secondary); font-size: 12px; }
.form-tip {
  margin: -2px 0 12px 110px;
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
