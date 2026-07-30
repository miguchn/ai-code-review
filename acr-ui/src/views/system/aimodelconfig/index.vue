<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
         <el-form-item label="模型名称" prop="modelName">
            <el-input
               v-model="queryParams.modelName"
               placeholder="请输入模型名称"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="模型厂商" prop="provider">
            <el-select v-model="queryParams.provider" placeholder="请选择厂商" clearable style="width: 160px">
               <el-option
                  v-for="item in providerOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
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
         <el-table-column label="模型名称" align="center" prop="modelName" :show-overflow-tooltip="true" />
         <el-table-column label="厂商" align="center" prop="provider" width="100" />
         <el-table-column label="Model" align="center" prop="model" :show-overflow-tooltip="true" />
         <el-table-column label="API 地址" align="center" prop="apiUrl" :show-overflow-tooltip="true" />
         <el-table-column label="API Key" align="center" prop="apiKey" width="120" :show-overflow-tooltip="true" />
         <el-table-column label="超时(ms)" align="center" prop="timeout" width="90" />
         <el-table-column label="最大Token" align="center" prop="maxTokens" width="100" />
         <el-table-column label="排序" align="center" prop="sortOrder" width="70" />
         <el-table-column label="状态" align="center" width="80">
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
         <el-table-column label="默认" align="center" width="70">
            <template #default="scope">
               <el-tag v-if="scope.row.isDefault === '1'" type="success" size="small">默认</el-tag>
               <el-button v-else link type="primary" size="small" @click="handleSetDefault(scope.row)" v-hasPermi="['system:aimodelconfig:edit']">设为默认</el-button>
            </template>
         </el-table-column>
         <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
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

      <!-- 添加或修改对话框 -->
      <el-dialog :title="title" v-model="open" width="680px" append-to-body>
         <el-form ref="modelRef" :model="form" :rules="rules" label-width="100px">
            <el-row :gutter="20">
               <el-col :span="12">
                  <el-form-item label="模型名称" prop="modelName">
                     <el-input v-model="form.modelName" placeholder="如: GPT-4o" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="模型厂商" prop="provider">
                     <el-select v-model="form.provider" placeholder="请选择厂商" style="width: 100%">
                        <el-option
                           v-for="item in providerOptions"
                           :key="item.value"
                           :label="item.label"
                           :value="item.value"
                        />
                     </el-select>
                  </el-form-item>
               </el-col>
            </el-row>
            <el-form-item label="模型地址" prop="apiUrl">
               <el-input v-model="form.apiUrl" placeholder="如: https://api.openai.com/v1/chat/completions" />
            </el-form-item>
            <el-form-item label="API Key" prop="apiKey">
               <el-input v-model="form.apiKey" placeholder="请输入 API Key" show-password />
            </el-form-item>
            <el-row :gutter="20">
               <el-col :span="12">
                  <el-form-item label="Model" prop="model">
                     <el-input v-model="form.model" placeholder="如: gpt-4o" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="Embedding" prop="embeddingModel">
                     <el-input v-model="form.embeddingModel" placeholder="如: text-embedding-3-small，空则按厂商默认" />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-form-item label="Embedding URL" prop="embeddingApiUrl">
               <el-input v-model="form.embeddingApiUrl" placeholder="可选，空则从模型地址自动推导 .../v1/embeddings" />
            </el-form-item>
            <el-row :gutter="20">
               <el-col :span="12">
                  <el-form-item label="排序" prop="sortOrder">
                     <el-input-number v-model="form.sortOrder" :min="0" :max="999" style="width: 100%" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="超时时间(ms)" prop="timeout">
                     <el-input-number v-model="form.timeout" :min="1000" :max="300000" :step="10000" style="width: 100%" />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row :gutter="20">
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
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button @click="handleTestConnection" :loading="testingConnection">测 试</el-button>
               <el-button type="primary" @click="submitForm">确 定</el-button>
               <el-button @click="cancel">取 消</el-button>
            </div>
         </template>
      </el-dialog>
   </div>
</template>

<script setup name="SysAiModelConfig">
import { listAiModelConfig, getAiModelConfig, delAiModelConfig, addAiModelConfig, updateAiModelConfig, enableAiModel, setDefaultModel, testAiModel } from "@/api/system/aiModelConfig"

const providerOptions = [
   { label: 'OpenAI', value: 'openai' },
   { label: 'Anthropic', value: 'anthropic' },
   { label: '通义千问', value: 'qwen' },
   { label: 'DeepSeek', value: 'deepseek' },
   { label: '自定义', value: 'custom' }
]

const { proxy } = getCurrentInstance()

const modelList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const title = ref("")
const testingConnection = ref(false)

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
      modelName: [{ required: true, message: "模型名称不能为空", trigger: "blur" }],
      provider: [{ required: true, message: "模型厂商不能为空", trigger: "change" }],
      apiUrl: [{ required: true, message: "模型地址不能为空", trigger: "blur" }]
   }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询列表 */
function getList() {
   loading.value = true
   listAiModelConfig(queryParams.value).then(response => {
      modelList.value = response.rows
      total.value = response.total
      loading.value = false
   })
}

/** 取消按钮 */
function cancel() {
   open.value = false
   reset()
}

/** 表单重置 */
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
      sortOrder: 0,
      remark: undefined
   }
   proxy.resetForm("modelRef")
}

/** 搜索按钮操作 */
function handleQuery() {
   queryParams.value.pageNum = 1
   getList()
}

/** 重置按钮操作 */
function resetQuery() {
   proxy.resetForm("queryRef")
   handleQuery()
}

/** 新增按钮操作 */
function handleAdd() {
   reset()
   open.value = true
   title.value = "新增 AI 大模型配置"
}

/** 修改按钮操作 */
function handleUpdate(row) {
   reset()
   getAiModelConfig(row.modelId).then(response => {
      form.value = response.data
      open.value = true
      title.value = "修改 AI 大模型配置"
   })
}

/** 提交按钮 */
function submitForm() {
   proxy.$refs["modelRef"].validate(valid => {
      if (valid) {
         if (form.value.modelId != undefined) {
            updateAiModelConfig(form.value).then(() => {
               proxy.$modal.msgSuccess("修改成功")
               open.value = false
               getList()
            })
         } else {
            addAiModelConfig(form.value).then(() => {
               proxy.$modal.msgSuccess("新增成功")
               open.value = false
               getList()
            })
         }
      }
   })
}

/** 删除按钮操作 */
function handleDelete(row) {
   proxy.$modal.confirm('是否确认删除该 AI 模型配置？').then(() => {
      return delAiModelConfig(row.modelId)
   }).then(() => {
      getList()
      proxy.$modal.msgSuccess("删除成功")
   }).catch(() => {})
}

/** 启用/禁用切换 */
function handleEnableChange(row) {
   enableAiModel(row.modelId, row.enabled).then(() => {
      proxy.$modal.msgSuccess(row.enabled === "1" ? "已启用" : "已禁用")
   }).catch(() => {
      row.enabled = row.enabled === "1" ? "0" : "1"
   })
}

/** 设为默认模型 */
function handleSetDefault(row) {
   setDefaultModel(row.modelId).then(() => {
      proxy.$modal.msgSuccess("已设为默认模型")
      getList()
   })
}

/** 测试连接 */
function handleTestConnection() {
   if (!form.value.provider || !form.value.apiUrl || !form.value.apiKey) {
      proxy.$modal.msgWarning("请先填写模型厂商、API 地址和 API Key")
      return
   }
   testingConnection.value = true
   testAiModel({
      provider: form.value.provider,
      apiUrl: form.value.apiUrl,
      apiKey: form.value.apiKey,
      model: form.value.model || undefined,
      timeout: form.value.timeout || 60000
   }).then(response => {
      testingConnection.value = false
      proxy.$modal.msgSuccess("连接测试成功: " + response.data)
   }).catch(() => {
      testingConnection.value = false
   })
}

getList()
</script>
