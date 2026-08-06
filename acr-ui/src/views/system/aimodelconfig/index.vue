<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
         <el-form-item label="配置名称" prop="modelName">
            <el-input
               v-model="queryParams.modelName"
               placeholder="请输入配置名称"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="服务厂商" prop="provider">
            <el-select v-model="queryParams.provider" placeholder="请选择厂商" clearable style="width: 160px">
               <el-option
                  v-for="item in providerOptions"
                  :key="item.code"
                  :label="item.label"
                  :value="item.code"
               />
            </el-select>
         </el-form-item>
         <el-form-item label="状态" prop="enabled">
            <el-select v-model="queryParams.enabled" placeholder="请选择状态" clearable style="width: 120px">
               <el-option label="启用" value="1" />
               <el-option label="禁用" value="0" />
            </el-select>
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button
               type="primary"
               plain
               icon="Plus"
               @click="handleAdd"
               v-hasPermi="['system:aimodelconfig:add']"
            >新增</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="modelList">
         <el-table-column label="配置名称" align="center" prop="modelName" :show-overflow-tooltip="true" />
         <el-table-column label="服务厂商" align="center" prop="provider" width="160">
            <template #default="scope">
               <div class="provider-cell">
                  <img v-if="scope.row.provider" :src="providerIconUrl(scope.row.provider)" :alt="scope.row.provider" class="provider-icon" />
                  <span>{{ providerLabel(providerOptions, scope.row.provider, scope.row.customProviderName) }}</span>
               </div>
            </template>
         </el-table-column>
         <el-table-column label="模型标识" align="center" prop="model" :show-overflow-tooltip="true" />
         <el-table-column label="启用状态" align="center" width="90">
            <template #default="scope">
               <el-switch
                  v-model="scope.row.enabled"
                  active-value="1"
                  inactive-value="0"
                  @change="handleEnableChange(scope.row)"
                  v-hasPermi="['system:aimodelconfig:edit']"
               />
            </template>
         </el-table-column>
         <el-table-column label="默认状态" align="center" width="90">
            <template #default="scope">
               <el-tag v-if="scope.row.isDefault === '1'" type="success" size="small">默认</el-tag>
               <el-button v-else link type="primary" size="small" @click="handleSetDefault(scope.row)" v-hasPermi="['system:aimodelconfig:edit']">设为默认</el-button>
            </template>
         </el-table-column>
         <el-table-column label="最近检测结果" align="center" prop="lastCheckResult" min-width="160" :show-overflow-tooltip="true" />
         <el-table-column label="最近检测时间" align="center" prop="lastCheckTime" width="170">
            <template #default="scope">
               <span>{{ formatDateTime(scope.row.lastCheckTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:aimodelconfig:edit']">修改</el-button>
               <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:aimodelconfig:remove']">删除</el-button>
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

      <el-dialog :title="title" v-model="open" width="640px" append-to-body class="model-config-dialog" destroy-on-close>
         <el-form ref="modelRef" :model="form" :rules="rules" label-width="100px" class="model-config-form">
            <div class="guide-deep-link">
               <el-button link type="primary" size="small" @click="guideStore.open('model-config')">查看模型配置指引 →</el-button>
            </div>
            <div class="form-section-title">基础配置</div>
            <el-form-item label="配置名称" prop="modelName">
               <el-input v-model="form.modelName" placeholder="如: DeepSeek 生产" maxlength="64" />
            </el-form-item>
            <el-form-item label="服务厂商" prop="provider">
               <el-select v-model="form.provider" placeholder="请选择厂商" style="width: 100%" @change="handleProviderChange">
                  <el-option
                     v-for="item in providerOptions"
                     :key="item.code"
                     :label="item.label"
                     :value="item.code"
                  >
                     <div class="provider-option">
                        <img :src="providerIconUrl(item.code)" :alt="item.code" class="provider-icon" />
                        <span>{{ item.label }}</span>
                     </div>
                  </el-option>
               </el-select>
            </el-form-item>
            <el-form-item v-if="form.provider === 'custom'" label="厂商名称" prop="customProviderName">
               <el-input v-model="form.customProviderName" placeholder="自定义厂商显示名称" maxlength="64" />
            </el-form-item>
            <el-form-item label="API 地址" prop="apiUrl">
               <el-input v-model="form.apiUrl" placeholder="OpenAI Compatible Chat Completions 地址" />
            </el-form-item>
            <el-form-item label="模型标识" prop="model">
               <el-input v-model="form.model" placeholder="如: deepseek-chat" maxlength="64" />
            </el-form-item>
            <el-form-item label="API Key" prop="apiKey">
               <el-input v-model="form.apiKey" :placeholder="form.modelId ? '留空则不修改' : '请输入 API Key'" show-password />
            </el-form-item>
            <el-row :gutter="16">
               <el-col :span="12">
                  <el-form-item label="是否启用" prop="enabled">
                     <el-radio-group v-model="form.enabled">
                        <el-radio value="1">启用</el-radio>
                        <el-radio value="0">禁用</el-radio>
                     </el-radio-group>
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="是否默认" prop="isDefault">
                     <el-radio-group v-model="form.isDefault">
                        <el-radio value="1">是</el-radio>
                        <el-radio value="0">否</el-radio>
                     </el-radio-group>
                  </el-form-item>
               </el-col>
            </el-row>

            <el-collapse v-model="advancedActive" class="advanced-collapse">
               <el-collapse-item title="高级设置" name="advanced">
                  <el-row :gutter="16">
                     <el-col :span="12">
                        <el-form-item label="超时(ms)" prop="timeout">
                           <el-input-number v-model="form.timeout" :min="1000" :max="300000" :step="10000" controls-position="right" style="width: 100%" />
                        </el-form-item>
                     </el-col>
                     <el-col :span="12">
                        <el-form-item label="最大 Token" prop="maxTokens">
                           <el-input-number v-model="form.maxTokens" :min="100" :max="200000" :step="1000" controls-position="right" style="width: 100%" />
                        </el-form-item>
                     </el-col>
                     <el-col :span="12">
                        <el-form-item label="Temperature" prop="temperature">
                           <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" controls-position="right" style="width: 100%" />
                        </el-form-item>
                     </el-col>
                     <el-col :span="12">
                        <el-form-item label="上下文长度" prop="contextLength">
                           <el-input-number v-model="form.contextLength" :min="1024" :max="1000000" :step="1024" controls-position="right" style="width: 100%" />
                        </el-form-item>
                     </el-col>
                     <el-col :span="12">
                        <el-form-item label="排序" prop="sortOrder">
                           <el-input-number v-model="form.sortOrder" :min="0" :max="999" controls-position="right" style="width: 100%" />
                        </el-form-item>
                     </el-col>
                     <el-col :span="24">
                        <el-form-item label="备注" prop="remark">
                           <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选备注" maxlength="500" />
                        </el-form-item>
                     </el-col>
                  </el-row>
               </el-collapse-item>
            </el-collapse>

            <el-alert v-if="testResult" :title="testResultTitle" :type="testResult.success ? 'success' : 'error'" :closable="false" show-icon class="test-result-alert">
               <template #default>
                  <div v-if="testResult.latencyMs != null">时延: {{ testResult.latencyMs }} ms</div>
                  <div v-if="testResult.errorType">错误类型: {{ testResult.errorType }}</div>
                  <div v-if="testResult.errorMessage">{{ testResult.errorMessage }}</div>
                  <div v-if="testResult.content">响应: {{ testResult.content }}</div>
                  <div v-if="testResult.rawSnippet" class="raw-snippet">摘要: {{ testResult.rawSnippet }}</div>
               </template>
            </el-alert>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button @click="handleTestConnection" :loading="testingConnection">连接测试</el-button>
               <el-button @click="handleTestModelCall" :loading="testingModelCall">模型调用测试</el-button>
               <el-button type="primary" @click="submitForm">确 定</el-button>
               <el-button @click="cancel">取 消</el-button>
            </div>
         </template>
      </el-dialog>
   </div>
</template>

<script setup name="SysAiModelConfig">
import { listAiModelConfig, getAiModelConfig, delAiModelConfig, addAiModelConfig, updateAiModelConfig, enableAiModel, setDefaultModel, testAiModelConnection, testAiModelCall, listLlmProviders } from "@/api/system/aiModelConfig"
import { LLM_PROVIDER_FALLBACK, LLM_ADVANCED_DEFAULTS, providerIconUrl, providerLabel, syncProviderFields } from "@/constants/llmProviders"
import useGuideStore from '@/store/modules/guide'

const { proxy } = getCurrentInstance()
const guideStore = useGuideStore()

const providerOptions = ref([...LLM_PROVIDER_FALLBACK])
const modelList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const title = ref("")
const testingConnection = ref(false)
const testingModelCall = ref(false)
const testResult = ref(null)
const testResultTitle = ref("")
const advancedActive = ref([])

const validateCustomProviderName = (rule, value, callback) => {
   if (form.value.provider === 'custom' && !value) {
      callback(new Error('自定义厂商名称不能为空'))
      return
   }
   callback()
}

const data = reactive({
   form: {},
   queryParams: {
      pageNum: 1,
      pageSize: 10,
      modelName: undefined,
      provider: undefined,
      enabled: undefined
   },
   rules: {
      modelName: [{ required: true, message: "配置名称不能为空", trigger: "blur" }],
      provider: [{ required: true, message: "服务厂商不能为空", trigger: "change" }],
      customProviderName: [{ validator: validateCustomProviderName, trigger: "blur" }],
      apiUrl: [{ required: true, message: "API 地址不能为空", trigger: "blur" }],
      model: [{ required: true, message: "模型标识不能为空", trigger: "blur" }]
   }
})

const { queryParams, form, rules } = toRefs(data)

function mergeProviders(remoteProviders) {
   const byCode = new Map(LLM_PROVIDER_FALLBACK.map(item => [item.code, { ...item }]))
   for (const item of remoteProviders || []) {
      if (!item || !item.code) {
         continue
      }
      byCode.set(item.code, {
         ...byCode.get(item.code),
         ...item
      })
   }
   // 保证「其他/自定义」始终存在，即使后端尚未升级到含 custom 的枚举
   if (!byCode.has('custom')) {
      byCode.set('custom', { code: 'custom', label: '其他/自定义', domestic: false })
   }
   const order = LLM_PROVIDER_FALLBACK.map(item => item.code)
   return [
      ...order.filter(code => byCode.has(code)).map(code => byCode.get(code)),
      ...[...byCode.keys()].filter(code => !order.includes(code)).map(code => byCode.get(code))
   ]
}

function loadProviders() {
   listLlmProviders().then(response => {
      providerOptions.value = mergeProviders(response.data)
   }).catch(() => {
      providerOptions.value = mergeProviders([])
   })
}

function getList() {
   loading.value = true
   listAiModelConfig(queryParams.value).then(response => {
      modelList.value = response.rows
      total.value = response.total
      loading.value = false
   })
}

function cancel() {
   open.value = false
   reset()
}

function reset() {
   form.value = {
      modelId: undefined,
      modelName: undefined,
      provider: undefined,
      customProviderName: undefined,
      apiUrl: undefined,
      apiKey: undefined,
      model: undefined,
      embeddingModel: undefined,
      embeddingApiUrl: undefined,
      enabled: LLM_ADVANCED_DEFAULTS.enabled,
      isDefault: LLM_ADVANCED_DEFAULTS.isDefault,
      timeout: LLM_ADVANCED_DEFAULTS.timeout,
      maxTokens: LLM_ADVANCED_DEFAULTS.maxTokens,
      temperature: LLM_ADVANCED_DEFAULTS.temperature,
      contextLength: LLM_ADVANCED_DEFAULTS.contextLength,
      sortOrder: LLM_ADVANCED_DEFAULTS.sortOrder,
      remark: undefined
   }
   advancedActive.value = []
   testResult.value = null
   testResultTitle.value = ""
   proxy.resetForm("modelRef")
}

function handleProviderChange(providerCode) {
   syncProviderFields(form.value, providerCode)
   // 厂商变更后旧检测结果不再有效
   testResult.value = null
   testResultTitle.value = ""
   if (proxy.$refs["modelRef"]) {
      proxy.$refs["modelRef"].clearValidate(['apiUrl', 'model', 'customProviderName'])
   }
}

function handleQuery() {
   queryParams.value.pageNum = 1
   getList()
}

function resetQuery() {
   proxy.resetForm("queryRef")
   handleQuery()
}

function handleAdd() {
   reset()
   open.value = true
   title.value = "新增大模型配置"
}

function handleUpdate(row) {
   reset()
   getAiModelConfig(row.modelId).then(response => {
      form.value = {
         ...form.value,
         ...response.data,
         apiKey: undefined
      }
      open.value = true
      title.value = "修改大模型配置"
   })
}

function submitForm() {
   proxy.$refs["modelRef"].validate(valid => {
      if (valid) {
         const payload = { ...form.value }
         if (payload.provider !== 'custom') {
            payload.customProviderName = ''
         }
         if (payload.modelId != undefined) {
            updateAiModelConfig(payload).then(() => {
               proxy.$modal.msgSuccess("修改成功")
               open.value = false
               getList()
            })
         } else {
            addAiModelConfig(payload).then(() => {
               proxy.$modal.msgSuccess("新增成功")
               open.value = false
               getList()
            })
         }
      }
   })
}

function handleDelete(row) {
   proxy.$modal.confirm('是否确认删除该大模型配置？').then(() => {
      return delAiModelConfig(row.modelId)
   }).then(() => {
      getList()
      proxy.$modal.msgSuccess("删除成功")
   }).catch(() => {})
}

function handleEnableChange(row) {
   enableAiModel(row.modelId, row.enabled).then(() => {
      proxy.$modal.msgSuccess(row.enabled === "1" ? "已启用" : "已禁用")
   }).catch(() => {
      row.enabled = row.enabled === "1" ? "0" : "1"
   })
}

function handleSetDefault(row) {
   setDefaultModel(row.modelId).then(() => {
      proxy.$modal.msgSuccess("已设为默认模型")
      getList()
   })
}

function validateTestForm() {
   if (!form.value.provider || !form.value.apiUrl || !form.value.model) {
      proxy.$modal.msgWarning("请先填写服务厂商、API 地址和模型标识")
      return false
   }
   if (form.value.provider === 'custom' && !form.value.customProviderName) {
      proxy.$modal.msgWarning("请先填写自定义厂商名称")
      return false
   }
   if (!form.value.modelId && !form.value.apiKey) {
      proxy.$modal.msgWarning("新增配置时请先填写 API Key")
      return false
   }
   return true
}

function showTestResult(result, resultTitle) {
   testResult.value = result
   testResultTitle.value = resultTitle
}

function handleTestConnection() {
   if (!validateTestForm()) return
   testingConnection.value = true
   testAiModelConnection({ ...form.value }).then(response => {
      showTestResult(response.data, response.data.success ? "连接测试成功" : "连接测试失败")
   }).finally(() => {
      testingConnection.value = false
   })
}

function handleTestModelCall() {
   if (!validateTestForm()) return
   testingModelCall.value = true
   testAiModelCall({ ...form.value }).then(response => {
      showTestResult(response.data, response.data.success ? "模型调用成功" : "模型调用失败")
   }).finally(() => {
      testingModelCall.value = false
   })
}

loadProviders()
getList()
</script>

<style scoped>
.provider-cell,
.provider-option {
   display: inline-flex;
   align-items: center;
   gap: 6px;
}
.provider-icon {
   width: 18px;
   height: 18px;
   object-fit: contain;
}
.form-section-title {
   margin: 0 0 12px;
   font-size: 13px;
   font-weight: 600;
   color: var(--el-text-color-regular);
}
.advanced-collapse {
   border: none;
   margin-bottom: 8px;
}
.advanced-collapse :deep(.el-collapse-item__header) {
   height: 36px;
   line-height: 36px;
   font-size: 13px;
   font-weight: 500;
   color: var(--el-text-color-regular);
   border-bottom: 1px solid var(--el-border-color-lighter);
}
.advanced-collapse :deep(.el-collapse-item__wrap) {
   border-bottom: none;
}
.advanced-collapse :deep(.el-collapse-item__content) {
   padding-top: 12px;
   padding-bottom: 0;
}
.test-result-alert {
   margin-top: 8px;
}
.raw-snippet {
   word-break: break-all;
   font-size: 12px;
   color: var(--el-text-color-secondary);
}
.guide-deep-link {
   margin: -8px 0 10px;
   line-height: 22px;
}
</style>

<style>
.model-config-dialog .el-dialog__body {
   max-height: min(68vh, 560px);
   overflow-y: auto;
   padding-top: 12px;
   padding-bottom: 8px;
}
</style>
