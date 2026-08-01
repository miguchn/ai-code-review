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
         <el-table-column label="服务厂商" align="center" prop="provider" width="140">
            <template #default="scope">
               <div class="provider-cell">
                  <img v-if="scope.row.provider" :src="providerIconUrl(scope.row.provider)" :alt="scope.row.provider" class="provider-icon" />
                  <span>{{ providerLabel(providerOptions, scope.row.provider) }}</span>
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
               <span>{{ parseTime(scope.row.lastCheckTime) }}</span>
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

      <el-dialog :title="title" v-model="open" width="720px" append-to-body>
         <el-form ref="modelRef" :model="form" :rules="rules" label-width="110px">
            <el-row :gutter="20">
               <el-col :span="12">
                  <el-form-item label="配置名称" prop="modelName">
                     <el-input v-model="form.modelName" placeholder="如: DeepSeek 生产" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="服务厂商" prop="provider">
                     <el-select v-model="form.provider" placeholder="请选择厂商" style="width: 100%">
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
               </el-col>
            </el-row>
            <el-form-item label="模型地址" prop="apiUrl">
               <el-input v-model="form.apiUrl" placeholder="如: https://api.deepseek.com/v1/chat/completions" />
            </el-form-item>
            <el-form-item label="API Key" prop="apiKey">
               <el-input v-model="form.apiKey" :placeholder="form.modelId ? '留空则不修改' : '请输入 API Key'" show-password />
            </el-form-item>
            <el-row :gutter="20">
               <el-col :span="12">
                  <el-form-item label="模型标识" prop="model">
                     <el-input v-model="form.model" placeholder="如: deepseek-chat" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="Embedding" prop="embeddingModel">
                     <el-input v-model="form.embeddingModel" placeholder="可选" />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-form-item label="Embedding URL" prop="embeddingApiUrl">
               <el-input v-model="form.embeddingApiUrl" placeholder="可选，空则从模型地址推导" />
            </el-form-item>
            <el-row :gutter="20">
               <el-col :span="8">
                  <el-form-item label="Temperature" prop="temperature">
                     <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" style="width: 100%" />
                  </el-form-item>
               </el-col>
               <el-col :span="8">
                  <el-form-item label="上下文长度" prop="contextLength">
                     <el-input-number v-model="form.contextLength" :min="1024" :max="1000000" :step="1024" style="width: 100%" />
                  </el-form-item>
               </el-col>
               <el-col :span="8">
                  <el-form-item label="排序" prop="sortOrder">
                     <el-input-number v-model="form.sortOrder" :min="0" :max="999" style="width: 100%" />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row :gutter="20">
               <el-col :span="12">
                  <el-form-item label="超时时间(ms)" prop="timeout">
                     <el-input-number v-model="form.timeout" :min="1000" :max="300000" :step="10000" style="width: 100%" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="最大 Token" prop="maxTokens">
                     <el-input-number v-model="form.maxTokens" :min="100" :max="200000" :step="1000" style="width: 100%" />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row :gutter="20">
               <el-col :span="12">
                  <el-form-item label="是否启用" prop="enabled">
                     <el-radio-group v-model="form.enabled">
                        <el-radio value="1">启用</el-radio>
                        <el-radio value="0">禁用</el-radio>
                     </el-radio-group>
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="默认模型" prop="isDefault">
                     <el-radio-group v-model="form.isDefault">
                        <el-radio value="1">是</el-radio>
                        <el-radio value="0">否</el-radio>
                     </el-radio-group>
                  </el-form-item>
               </el-col>
            </el-row>
            <el-form-item label="备注" prop="remark">
               <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
            </el-form-item>

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
import { LLM_PROVIDER_FALLBACK, providerIconUrl, providerLabel } from "@/constants/llmProviders"

const { proxy } = getCurrentInstance()

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
      apiUrl: [{ required: true, message: "模型地址不能为空", trigger: "blur" }],
      model: [{ required: true, message: "模型标识不能为空", trigger: "blur" }]
   }
})

const { queryParams, form, rules } = toRefs(data)

function loadProviders() {
   listLlmProviders().then(response => {
      if (response.data && response.data.length) {
         providerOptions.value = response.data
      }
   }).catch(() => {})
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
      apiUrl: undefined,
      apiKey: undefined,
      model: undefined,
      embeddingModel: undefined,
      embeddingApiUrl: undefined,
      enabled: "0",
      isDefault: "0",
      timeout: 60000,
      maxTokens: 8000,
      temperature: 0.7,
      contextLength: 128000,
      sortOrder: 0,
      remark: undefined
   }
   testResult.value = null
   testResultTitle.value = ""
   proxy.resetForm("modelRef")
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
   title.value = "新增 AI 大模型配置"
}

function handleUpdate(row) {
   reset()
   getAiModelConfig(row.modelId).then(response => {
      form.value = response.data
      form.value.apiKey = undefined
      open.value = true
      title.value = "修改 AI 大模型配置"
   })
}

function submitForm() {
   proxy.$refs["modelRef"].validate(valid => {
      if (valid) {
         const payload = { ...form.value }
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
   proxy.$modal.confirm('是否确认删除该 AI 模型配置？').then(() => {
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
      proxy.$modal.msgWarning("请先填写服务厂商、模型地址和模型标识")
      return false
   }
   if (!form.value.modelId && !form.value.apiKey) {
      proxy.$modal.msgWarning("新增配置时请先填写 API Key")
      return false
   }
   return true
}

function showTestResult(result, title) {
   testResult.value = result
   testResultTitle.value = title
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
.test-result-alert {
   margin-top: 8px;
}
.raw-snippet {
   word-break: break-all;
   font-size: 12px;
   color: var(--el-text-color-secondary);
}
</style>
