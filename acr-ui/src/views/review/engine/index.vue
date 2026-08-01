<template>
  <div class="app-container">
    <el-card shadow="never" class="engine-card">
      <template #header>
        <div class="card-header">
          <span>审查引擎</span>
          <div class="header-actions">
            <el-button type="primary" plain icon="Connection" :loading="detecting"
              @click="handleDetect" v-hasPermi="['review:engine:detect']">环境检测</el-button>
            <el-button type="primary" icon="VideoPlay" :loading="testing"
              @click="handleTest" v-hasPermi="['review:engine:test']">测试调用</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border v-loading="loading">
        <el-descriptions-item label="引擎名称">{{ engineInfo.engineName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="引擎类型">{{ engineInfo.engineType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="可执行文件">{{ engineInfo.executablePath || '-' }}</el-descriptions-item>
        <el-descriptions-item label="配置来源">{{ engineInfo.configSource || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前版本">{{ engineInfo.version || '未检测' }}</el-descriptions-item>
        <el-descriptions-item label="可用状态">
          <el-tag :type="availabilityTagType(engineInfo.availabilityStatus)" size="small">
            {{ availabilityText(engineInfo.availabilityStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="工作目录根路径" :span="2">{{ engineInfo.workspaceRoot || '-' }}</el-descriptions-item>
        <el-descriptions-item label="默认超时">{{ engineInfo.defaultTimeoutSeconds }} 秒</el-descriptions-item>
        <el-descriptions-item label="最大并发">{{ engineInfo.maxConcurrency }}</el-descriptions-item>
        <el-descriptions-item label="输出上限">{{ formatBytes(engineInfo.maxOutputBytes) }}</el-descriptions-item>
        <el-descriptions-item label="最近检测">
          <div class="status-line">
            <el-tag :type="engineInfo.lastDetectSuccess ? 'success' : 'info'" size="small">
              {{ engineInfo.lastDetectTime ? (engineInfo.lastDetectSuccess ? '成功' : '失败') : '未检测' }}
            </el-tag>
            <span v-if="engineInfo.lastDetectTime" class="status-time">{{ formatDateTime(engineInfo.lastDetectTime) }}</span>
          </div>
          <div v-if="engineInfo.lastDetectMessage" class="status-message">{{ engineInfo.lastDetectMessage }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="最近测试">
          <div class="status-line">
            <el-tag :type="engineInfo.lastTestSuccess ? 'success' : 'info'" size="small">
              {{ engineInfo.lastTestTime ? (engineInfo.lastTestSuccess ? '成功' : '失败') : '未测试' }}
            </el-tag>
            <span v-if="engineInfo.lastTestTime" class="status-time">{{ formatDateTime(engineInfo.lastTestTime) }}</span>
          </div>
          <div v-if="engineInfo.lastTestMessage" class="status-message">{{ engineInfo.lastTestMessage }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="lastResult" shadow="never" class="result-card">
      <template #header>
        <span>最近一次调用结果</span>
      </template>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="是否成功">
          <el-tag :type="lastResult.success ? 'success' : 'danger'" size="small">
            {{ lastResult.success ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="执行耗时">{{ lastResult.durationMs }} ms</el-descriptions-item>
        <el-descriptions-item label="引擎版本">{{ lastResult.engineVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="退出码">{{ lastResult.exitCode ?? '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="lastResult.failureType" label="失败类型">{{ lastResult.failureType }}</el-descriptions-item>
        <el-descriptions-item v-if="lastResult.failureReason" label="失败原因" :span="2">{{ lastResult.failureReason }}</el-descriptions-item>
      </el-descriptions>
      <el-collapse class="result-collapse">
        <el-collapse-item title="标准输出 (stdout)" name="stdout">
          <pre class="output-block">{{ lastResult.stdout || '(empty)' }}</pre>
        </el-collapse-item>
        <el-collapse-item title="标准错误 (stderr)" name="stderr">
          <pre class="output-block">{{ lastResult.stderr || '(empty)' }}</pre>
        </el-collapse-item>
        <el-collapse-item v-if="lastResult.structuredResult" title="结构化结果" name="structured">
          <pre class="output-block">{{ formatJson(lastResult.structuredResult) }}</pre>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <el-dialog title="测试调用" v-model="testDialogOpen" width="520px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="模型配置">
          <el-select v-model="testModelId" clearable placeholder="请选择已启用的模型配置" style="width: 100%">
            <el-option v-for="item in modelOptions" :key="item.modelId"
              :label="formatModelOption(item)"
              :value="item.modelId" />
          </el-select>
        </el-form-item>
        <el-alert
          :title="modelOptions.length ? '将使用所选模型配置调用审查引擎；未选择时使用当前默认模型。测试使用内置样例仓库，不会读取用户代码。' : '暂无已启用的模型配置，请先在「大模型配置」中新增并启用。'"
          :type="modelOptions.length ? 'info' : 'warning'"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="testDialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="testing" :disabled="!modelOptions.length" @click="submitTest">开始测试</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ReviewEngine">
import { getReviewEngineInfo, detectReviewEngine, testReviewEngine } from '@/api/review/engine'
import { listAiModelConfig } from '@/api/system/aiModelConfig'
import { LLM_PROVIDER_FALLBACK, providerLabel } from '@/constants/llmProviders'

const { proxy } = getCurrentInstance()

const loading = ref(true)
const detecting = ref(false)
const testing = ref(false)
const testDialogOpen = ref(false)
const testModelId = ref()
const engineInfo = ref({})
const lastResult = ref(null)
const modelOptions = ref([])

function loadInfo() {
  loading.value = true
  getReviewEngineInfo().then(res => {
    engineInfo.value = res.data || {}
  }).finally(() => {
    loading.value = false
  })
}

function loadModels() {
  return listAiModelConfig({ pageNum: 1, pageSize: 100, enabled: '1' }).then(res => {
    modelOptions.value = res.rows || []
    return modelOptions.value
  }).catch(() => {
    modelOptions.value = []
    return modelOptions.value
  })
}

function formatModelOption(item) {
  const provider = providerLabel(LLM_PROVIDER_FALLBACK, item.provider, item.customProviderName)
  const modelTag = item.model ? ` / ${item.model}` : ''
  const defaultTag = item.isDefault === '1' ? '（默认）' : ''
  return `${item.modelName} · ${provider}${modelTag}${defaultTag}`
}

function pickDefaultModelId(models) {
  if (!models || !models.length) {
    return undefined
  }
  const preferred = models.find(item => item.isDefault === '1')
  return preferred ? preferred.modelId : models[0].modelId
}

function handleDetect() {
  detecting.value = true
  detectReviewEngine().then(res => {
    lastResult.value = res.data
    proxy.$modal.msgSuccess(res.data?.success ? '环境检测通过' : '环境检测失败')
    loadInfo()
  }).finally(() => {
    detecting.value = false
  })
}

function handleTest() {
  testing.value = false
  loadModels().then(models => {
    testModelId.value = pickDefaultModelId(models)
    testDialogOpen.value = true
  })
}

function submitTest() {
  if (!testModelId.value && !modelOptions.value.length) {
    proxy.$modal.msgWarning('请先在「大模型配置」中配置并启用模型')
    return
  }
  testing.value = true
  const payload = testModelId.value != null ? { modelId: testModelId.value } : {}
  testReviewEngine(payload).then(res => {
    lastResult.value = res.data
    testDialogOpen.value = false
    proxy.$modal.msgSuccess(res.data?.success ? '测试调用成功' : '测试调用失败')
    loadInfo()
  }).finally(() => {
    testing.value = false
  })
}

function availabilityTagType(status) {
  if (status === 'AVAILABLE') return 'success'
  if (status === 'UNAVAILABLE') return 'danger'
  return 'info'
}

function availabilityText(status) {
  if (status === 'AVAILABLE') return '可用'
  if (status === 'UNAVAILABLE') return '不可用'
  return '未知'
}

function formatBytes(bytes) {
  if (!bytes) return '-'
  if (bytes >= 1048576) return (bytes / 1048576).toFixed(1) + ' MB'
  if (bytes >= 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return bytes + ' B'
}

function formatJson(value) {
  try {
    return JSON.stringify(value, null, 2)
  } catch (e) {
    return String(value)
  }
}

loadInfo()
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-actions {
  display: flex;
  gap: 8px;
}
.engine-card {
  margin-bottom: 16px;
}
.result-card {
  margin-bottom: 16px;
}
.status-line {
  display: flex;
  align-items: center;
  gap: 8px;
}
.status-time {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.status-message {
  margin-top: 4px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}
.output-block {
  margin: 0;
  padding: 12px;
  background: #f8faf9;
  border-radius: 6px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.5;
  max-height: 320px;
  overflow: auto;
}
.result-collapse {
  margin-top: 16px;
}
</style>
