<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
      <el-form-item label="项目名称" prop="projectName">
        <el-input v-model="queryParams.projectName" placeholder="请输入项目名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="业务系统" prop="businessSystemId">
        <el-select v-model="queryParams.businessSystemId" placeholder="请选择业务系统" clearable filterable style="width: 180px">
          <el-option v-for="item in options.businessSystems" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="Git 平台" prop="provider">
        <el-select v-model="queryParams.provider" placeholder="请选择平台" clearable style="width: 130px">
          <el-option label="GitHub" value="GITHUB" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['review:project:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="projectList" empty-text="暂无代码项目">
      <el-table-column label="项目名称" prop="projectName" min-width="150" :show-overflow-tooltip="true" />
      <el-table-column label="GitHub 仓库" min-width="220" :show-overflow-tooltip="true">
        <template #default="scope">
          <el-link :href="scope.row.repositoryUrl" target="_blank" type="primary" rel="noopener noreferrer">
            {{ scope.row.repositoryOwner }}/{{ scope.row.repositoryName }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="业务系统" prop="businessSystemName" min-width="130" :show-overflow-tooltip="true" />
      <el-table-column label="所属部门" prop="deptName" min-width="110" :show-overflow-tooltip="true" />
      <el-table-column label="负责人" prop="ownerName" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="默认分支" prop="defaultBranch" width="110" :show-overflow-tooltip="true" />
      <el-table-column label="主要语言" width="100">
        <template #default="scope">
          <dict-tag :options="review_tech_stack" :value="scope.row.primaryStack || 'FULLSTACK'" />
        </template>
      </el-table-column>
      <el-table-column label="审查方式" width="120">
        <template #default="scope">
          <dict-tag :options="review_mode" :value="normalizeReviewMode(scope.row.reviewMode)" />
        </template>
      </el-table-column>
      <el-table-column label="PR 审查" min-width="180">
        <template #default="scope">
          <el-tag :type="scope.row.prReviewEnabled === '0' ? 'success' : 'info'" size="small">
            {{ scope.row.prReviewEnabled === '0' ? '已启用' : '未启用' }}
          </el-tag>
          <div v-if="scope.row.prReviewEnabled === '0'" class="text-muted text-ellipsis">
            {{ formatTargetBranches(scope.row.prTargetBranches) }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="仓库同步" width="150">
        <template #default="scope">
          <el-tooltip :content="scope.row.lastBranchSyncMessage || '尚未同步分支'" placement="top">
            <el-tag :type="syncTagType(scope.row.lastBranchSyncStatus)" size="small">
              {{ syncStatusText(scope.row.lastBranchSyncStatus) }}
            </el-tag>
          </el-tooltip>
          <div v-if="scope.row.lastBranchSyncTime" class="text-muted">{{ formatDateTime(scope.row.lastBranchSyncTime) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="最近检测" width="150">
        <template #default="scope">
          <el-tooltip :content="scope.row.lastCheckMessage || '尚未检测'" placement="top">
            <el-tag :type="checkTagType(scope.row.lastCheckStatus)" size="small">
              {{ checkStatusText(scope.row.lastCheckStatus) }}
            </el-tag>
          </el-tooltip>
          <div v-if="scope.row.lastCheckTime" class="text-muted">{{ formatDateTime(scope.row.lastCheckTime) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态 / 操作" width="310" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="small" class="mr8">
            {{ scope.row.status === '0' ? '启用' : '停用' }}
          </el-tag>
          <el-button link type="primary" @click="handleStatusChange(scope.row)" v-hasPermi="['review:project:status']">
            {{ scope.row.status === '0' ? '停用' : '启用' }}
          </el-button>
          <el-button link type="primary" icon="Connection" :loading="testingId === scope.row.projectId"
            @click="handleTest(scope.row)" v-hasPermi="['review:project:test']">检测</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['review:project:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['review:project:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="920px" top="4vh" append-to-body
      @opened="scrollProjectFormToTop" @closed="reset">
      <el-form ref="projectRef" :model="form" :rules="rules" label-width="110px" class="project-form">
        <el-tabs v-model="activeTab" class="project-tabs">
          <el-tab-pane label="基础信息" name="basic">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="项目名称" prop="projectName">
                  <el-input v-model="form.projectName" placeholder="请输入企业内部项目名称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="Git 平台">
                  <el-input model-value="GitHub" disabled />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="业务系统" prop="businessSystemId">
                  <el-select v-model="form.businessSystemId" filterable placeholder="请选择业务系统" @change="handleSystemChange">
                    <el-option v-for="item in options.businessSystems" :key="item.id" :label="item.label" :value="item.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="所属部门" prop="deptId">
                  <el-select v-model="form.deptId" filterable disabled placeholder="选择业务系统后自动带出">
                    <el-option v-for="item in options.departments" :key="item.id" :label="item.label" :value="item.id" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="项目负责人" prop="ownerUserId">
                  <el-select v-model="form.ownerUserId" filterable placeholder="请选择负责人">
                    <el-option v-for="item in availableOwners" :key="item.id" :label="item.label" :value="item.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="项目状态">
                  <div class="form-control-block">
                    <el-tag :type="form.status === '0' ? 'success' : 'info'">{{ form.status === '0' ? '启用' : '停用' }}</el-tag>
                    <div class="inline-tip">
                      {{ form.projectId ? '项目启停请在列表中操作。' : '新项目默认停用，保存后可在列表中启用。' }}
                    </div>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="可选，例如项目用途或接入说明" />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="仓库与分支" name="repository">
            <el-form-item label="仓库地址" prop="repositoryUrl">
              <el-input v-model="form.repositoryUrl" placeholder="https://github.com/owner/repository" @change="invalidateRepositoryInfo" />
            </el-form-item>
            <el-form-item label="访问凭据" prop="credentialId">
              <div class="form-control-block">
                <el-select v-model="form.credentialId" filterable placeholder="请选择已有 GitHub 凭据" @change="invalidateRepositoryInfo">
                  <el-option v-for="item in options.credentials" :key="item.id" :label="item.label" :value="item.id" />
                </el-select>
                <div class="field-actions">
                  <el-button link type="primary" icon="Plus" @click="openCredentialManagement"
                    v-hasPermi="['review:credential:add']">新增凭据</el-button>
                  <el-button link type="primary" icon="Refresh" @click="refreshCredentialOptions">刷新列表</el-button>
                  <el-button type="primary" plain icon="Download" :loading="repositoryReading" @click="handleReadRepositoryInfo"
                    v-hasPermi="['review:project:test']">
                    {{ form.repositoryOwner ? '刷新分支' : '读取仓库信息' }}
                  </el-button>
                </div>
                <div v-if="form.lastBranchSyncStatus === 'FAILED'" class="inline-tip is-error">
                  <el-icon><WarningFilled /></el-icon><span>{{ form.lastBranchSyncMessage }}</span>
                </div>
                <div v-else-if="!form.repositoryOwner" class="inline-tip">
                  选择凭据后读取仓库信息，系统将自动获取仓库名称、默认分支和全部分支。
                </div>
              </div>
            </el-form-item>
            <el-form-item v-if="form.repositoryOwner" label="仓库信息">
              <div class="repository-summary">
                <div><span>仓库</span><strong>{{ form.repositoryOwner }}/{{ form.repositoryName }}</strong></div>
                <div><span>默认分支</span><strong>{{ form.defaultBranch || '-' }}</strong></div>
                <div><span>分支数量</span><strong>{{ branchCount ?? '刷新后显示' }}</strong></div>
                <div><span>最近同步</span><strong>{{ form.lastBranchSyncTime ? formatDateTime(form.lastBranchSyncTime) : '尚未同步' }}</strong></div>
              </div>
            </el-form-item>
            <el-form-item label="启用 PR 审查" prop="prReviewEnabled">
              <div class="form-control-block">
                <el-switch v-model="form.prReviewEnabled" active-value="0" inactive-value="1"
                  active-text="启用" inactive-text="停用" @change="handlePrReviewChange" />
                <div class="inline-tip">仅审查 Pull Request，不启用 Push 审查。</div>
              </div>
            </el-form-item>
            <el-form-item v-if="form.prReviewEnabled === '0'" label="目标分支" prop="prTargetBranches">
              <div class="form-control-block">
                <el-select v-model="form.prTargetBranches" multiple filterable collapse-tags :max-collapse-tags="2"
                  :placeholder="repositoryInfoLoaded ? '请选择 PR 目标分支' : '请先读取仓库信息'">
                  <el-option v-for="branch in branchOptions" :key="branch" :label="branch" :value="branch" />
                </el-select>
                <div class="field-actions field-actions--split">
                  <span class="inline-tip inline-tip--inline">来源分支默认全部允许，通常只选择 dev 或 develop，避免后续合并重复审查。</span>
                  <div class="field-actions__links">
                    <el-button link type="primary" :disabled="!repositoryInfoLoaded" @click="branchDialogOpen = true">查看全部分支</el-button>
                    <el-button link type="primary" :loading="repositoryReading" @click="handleReadRepositoryInfo"
                      v-hasPermi="['review:project:test']">刷新分支</el-button>
                  </div>
                </div>
              </div>
            </el-form-item>
            <el-collapse v-model="advancedSections" class="advanced-settings">
              <el-collapse-item title="高级设置（系统统一配置）" name="advanced">
                <el-form-item label="来源分支"><span>全部允许</span></el-form-item>
                <el-form-item label="机器人分支">
                  <el-tag v-for="item in options.robotBranchPrefixes" :key="item" type="info" size="small" class="mr8">{{ item }}</el-tag>
                </el-form-item>
                <el-form-item label="PR 触发事件">
                  <el-tag v-for="item in options.prEvents" :key="item" type="info" size="small" class="mr8">{{ item }}</el-tag>
                </el-form-item>
              </el-collapse-item>
            </el-collapse>
          </el-tab-pane>

          <el-tab-pane label="Webhook" name="webhook">
            <el-form-item label="说明">
              <div class="inline-tip">
                Webhook 用于接收 GitHub PR 事件并创建审查任务。Secret 加密保存，页面不回显明文。
              </div>
            </el-form-item>
            <el-form-item label="回调地址">
              <div class="form-control-block">
                <el-input :model-value="webhookCallbackDisplay" readonly>
                  <template #append>
                    <el-button :disabled="!webhookCallbackDisplay" @click="copyWebhookCallback">复制</el-button>
                  </template>
                </el-input>
                <div class="inline-tip">在 GitHub 仓库 Settings → Webhooks 中按此地址添加，Content type 选择 application/json。</div>
              </div>
            </el-form-item>
            <el-form-item label="Webhook Secret" prop="webhookSecret">
              <div class="form-control-block">
                <el-input v-model="form.webhookSecret" type="password" show-password clearable
                  :placeholder="form.projectId ? '留空保持不变，输入则更新' : '与 GitHub Webhook Secret 保持一致'" />
                <div class="inline-tip">
                  用于校验 GitHub 事件签名。
                  <el-tag :type="form.webhookSecretConfigured ? 'success' : 'info'" size="small">
                    {{ form.webhookSecretConfigured ? '已配置' : '未配置' }}
                  </el-tag>
                </div>
              </div>
            </el-form-item>
            <el-form-item v-if="form.projectId" label="最近接收">
              <span v-if="form.lastWebhookTime">{{ formatDateTime(form.lastWebhookTime) }} · {{ form.lastWebhookResult }}</span>
              <span v-else class="inline-tip">尚未接收 Webhook 事件</span>
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="审查范围" name="scope">
            <el-form-item label="说明">
              <div class="inline-tip">
                默认仅审查本次 PR 变更内容，不扫描整个文件的历史问题；锁文件、依赖目录与构建产物由平台统一排除。以下配置随任务快照冻结，修改仅影响新建任务。
              </div>
            </el-form-item>
            <el-form-item label="排除路径" prop="scopeExcludePatterns">
              <div class="form-control-block">
                <el-input v-model="form.scopeExcludePatterns" type="textarea" :rows="4" maxlength="2000" show-word-limit
                  :placeholder="'每行一个 glob 规则，例如：\ndocs/**\n*.generated.java\nsrc/main/resources/static/**'" />
                <div class="inline-tip">在平台默认排除之上追加，命中的文件不进入审查。留空表示不追加。</div>
              </div>
            </el-form-item>
            <el-form-item label="高影响扩展">
              <div class="form-control-block">
                <el-switch v-model="form.scopeExpandEnabled" active-value="Y" inactive-value="N"
                  active-text="启用" inactive-text="关闭" />
                <div class="inline-tip">
                  新增文件、公共方法/接口签名、权限安全逻辑、配置文件、依赖声明、数据库脚本变更时，自动扩展为整文件审查。建议保持启用。
                </div>
              </div>
            </el-form-item>
            <el-form-item label="审查测试文件">
              <div class="form-control-block">
                <el-switch v-model="form.scopeIncludeTests" active-value="Y" inactive-value="N"
                  active-text="审查" inactive-text="不审查" />
                <div class="inline-tip">关闭时自动排除 *Test.java、*_test.go、*.test.* 等测试文件的变更。</div>
              </div>
            </el-form-item>
            <el-form-item label="上报存量问题">
              <div class="form-control-block">
                <el-switch v-model="form.scopeReportExisting" active-value="Y" inactive-value="N"
                  active-text="上报" inactive-text="不上报" />
                <div class="inline-tip">开启后保留变更行之外的历史存量问题并标注来源；默认剔除，审查结果只聚焦本次变更。</div>
              </div>
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="审查执行" name="execution">
            <el-form-item label="说明">
              <div class="inline-tip">
                审查方式二选一：大模型审查使用模型服务 + 审查模板；审查引擎使用本机 open-code-review，不使用平台模板。
              </div>
            </el-form-item>
            <el-form-item label="主要语言" prop="primaryStack">
              <div class="form-control-block">
                <el-select v-model="form.primaryStack" placeholder="请选择项目主要语言/技术类型" @change="handlePrimaryStackChange">
                  <el-option v-for="dict in review_tech_stack" :key="dict.value" :label="dict.label" :value="dict.value" />
                </el-select>
                <div class="inline-tip">用于筛选推荐审查模板；不强制模板必须同技术栈。</div>
              </div>
            </el-form-item>
            <el-form-item label="审查方式" prop="reviewMode">
              <el-radio-group v-model="form.reviewMode" @change="handleReviewModeChange">
                <el-radio-button v-for="dict in reviewModeOptions" :key="dict.value" :value="dict.value">
                  {{ dict.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>

            <template v-if="isLlmDirectMode">
              <el-form-item label="模型配置" prop="modelId">
                <div class="form-control-block">
                  <el-select v-model="form.modelId" filterable
                    :placeholder="options.models.length ? '请选择已启用的模型配置' : '暂无可用模型，请先到模型服务配置'">
                    <el-option v-for="item in options.models" :key="item.id" :label="item.label" :value="item.id" />
                  </el-select>
                  <div class="inline-tip">
                    必选。建单时冻结模型快照，后续修改不影响已创建任务。
                    <el-button v-if="!options.models.length" link type="primary" @click="openModelService">前往模型服务</el-button>
                  </div>
                </div>
              </el-form-item>
              <el-form-item label="审查模板" prop="templateId">
                <div class="form-control-block">
                  <el-select v-model="form.templateId" filterable
                    :placeholder="filteredTemplates.length ? '请选择已启用的审查模板' : '暂无可用模板，请先到审查模板配置'">
                    <el-option v-for="item in filteredTemplates" :key="item.id"
                      :label="formatTemplateLabel(item)" :value="item.id" />
                  </el-select>
                  <div class="inline-tip">
                    必选。建单时冻结模板版本与正文；修改模板只影响新任务。
                    <el-button link type="primary" @click="showAllTemplates = !showAllTemplates">
                      {{ showAllTemplates ? '仅看同技术栈' : '显示全部模板' }}
                    </el-button>
                    <el-button v-if="!options.templates.length" link type="primary" @click="openTemplateManagement">前往审查模板</el-button>
                  </div>
                </div>
              </el-form-item>
            </template>

            <template v-else>
              <el-form-item label="审查引擎" prop="engineCode">
                <div class="form-control-block">
                  <el-select v-model="form.engineCode" placeholder="open-code-review">
                    <el-option v-for="dict in review_engine_code" :key="dict.value" :label="dict.label" :value="dict.value" />
                  </el-select>
                  <div class="inline-tip">
                    调用服务器已安装的 open-code-review。不使用平台审查模板。
                    <el-button link type="primary" @click="openEnginePage">查看审查引擎</el-button>
                  </div>
                  <div class="inline-tip">
                    当前方式不绑定项目级模型与审查模板。请确保「模型服务」已配置并启用平台默认模型（引擎运行依赖），且「审查引擎」环境检测通过。
                  </div>
                </div>
              </el-form-item>
            </template>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button v-if="activeTab !== 'basic'" @click="prevTab">上一步</el-button>
          <el-button v-if="activeTab !== 'execution'" type="primary" plain @click="nextTab">下一步</el-button>
          <el-button type="primary" :loading="submitting" @click="submitForm">保 存</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="GitHub 分支" v-model="branchDialogOpen" width="620px" append-to-body>
      <el-input v-model="branchSearch" clearable prefix-icon="Search" placeholder="搜索分支名称" class="mb12" />
      <el-table :data="filteredBranches" height="360" empty-text="未找到匹配分支">
        <el-table-column label="分支名称" prop="name" min-width="280" />
        <el-table-column label="标记" width="180">
          <template #default="scope">
            <el-tag v-if="scope.row.name === form.defaultBranch" type="info" size="small" class="mr8">默认分支</el-tag>
            <el-tag v-if="form.prTargetBranches.includes(scope.row.name)" type="success" size="small">已选择</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="branchDialogOpen = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ReviewProject">
import {
  listReviewProject, getReviewProject, getReviewProjectOptions, readReviewProjectRepositoryInfo,
  addReviewProject, updateReviewProject, delReviewProject, changeReviewProjectStatus, testReviewProject
} from '@/api/review/project'

const { proxy } = getCurrentInstance()
const router = useRouter()
const { review_mode, review_engine_code, review_tech_stack } = proxy.useDict('review_mode', 'review_engine_code', 'review_tech_stack')
const projectList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')
const testingId = ref()
const submitting = ref(false)
const repositoryReading = ref(false)
const repositoryInfoLoaded = ref(false)
const loadedRepositorySignature = ref('')
const originalRepositorySignature = ref('')
const availableBranches = ref([])
const recommendedBranches = ref([])
const branchCount = ref()
const branchDialogOpen = ref(false)
const branchSearch = ref('')
const advancedSections = ref([])
const activeTab = ref('basic')
const tabOrder = ['basic', 'repository', 'webhook', 'scope', 'execution']
const options = reactive({
  businessSystems: [], departments: [], owners: [], credentials: [], models: [], templates: [],
  longLivedBranches: [], robotBranchPrefixes: [], prEvents: [], webhookCallbackUrl: ''
})

function validateTargetBranches(rule, value, callback) {
  if (data.form.prReviewEnabled === '0' && (!Array.isArray(value) || value.length === 0)) {
    callback(new Error('请选择至少一个 PR 目标分支'))
    return
  }
  callback()
}

function validateModelId(rule, value, callback) {
  if (data.form.reviewMode === 'LLM_DIRECT' && !value) {
    callback(new Error('大模型审查必须选择模型配置'))
    return
  }
  callback()
}

function validateTemplateId(rule, value, callback) {
  if (data.form.reviewMode === 'LLM_DIRECT' && !value) {
    callback(new Error('大模型审查必须选择审查模板'))
    return
  }
  callback()
}

function validateEngineCode(rule, value, callback) {
  if (data.form.reviewMode === 'OCR_ENGINE' && !value) {
    callback(new Error('审查引擎方式必须选择引擎'))
    return
  }
  callback()
}

const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, projectName: undefined, businessSystemId: undefined, provider: undefined, status: undefined },
  rules: {
    projectName: [{ required: true, message: '项目名称不能为空', trigger: 'blur' }],
    repositoryUrl: [{ required: true, message: 'GitHub 仓库地址不能为空', trigger: 'blur' }],
    prTargetBranches: [{ validator: validateTargetBranches, trigger: 'change' }],
    businessSystemId: [{ required: true, message: '请选择业务系统', trigger: 'change' }],
    deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
    ownerUserId: [{ required: true, message: '请选择项目负责人', trigger: 'change' }],
    credentialId: [{ required: true, message: '请选择访问凭据', trigger: 'change' }],
    primaryStack: [{ required: true, message: '请选择项目主要语言/技术类型', trigger: 'change' }],
    reviewMode: [{ required: true, message: '请选择审查方式', trigger: 'change' }],
    modelId: [{ validator: validateModelId, trigger: 'change' }],
    templateId: [{ validator: validateTemplateId, trigger: 'change' }],
    engineCode: [{ validator: validateEngineCode, trigger: 'change' }]
  }
})
const { queryParams, form, rules } = toRefs(data)
const showAllTemplates = ref(false)
const availableOwners = computed(() => form.value.deptId
  ? options.owners.filter(item => item.deptId === form.value.deptId)
  : options.owners)
const branchOptions = computed(() => Array.from(new Set([
  ...availableBranches.value,
  ...(Array.isArray(form.value.prTargetBranches) ? form.value.prTargetBranches : [])
])))
const filteredBranches = computed(() => {
  const keyword = branchSearch.value.trim().toLowerCase()
  return availableBranches.value
    .filter(name => !keyword || name.toLowerCase().includes(keyword))
    .map(name => ({ name }))
})
const currentRepositorySignature = computed(() => repositorySignature(form.value))
const webhookCallbackDisplay = computed(() => form.value.webhookCallbackUrl || options.webhookCallbackUrl || '')
const reviewModeOptions = computed(() => (review_mode.value || []).filter(item =>
  item.value === 'LLM_DIRECT' || item.value === 'OCR_ENGINE'
))
const isLlmDirectMode = computed(() => form.value.reviewMode === 'LLM_DIRECT')
const filteredTemplates = computed(() => {
  const list = options.templates || []
  if (showAllTemplates.value || !form.value.primaryStack) return list
  const matched = list.filter(item => item.techStack === form.value.primaryStack)
  return matched.length ? matched : list
})

function copyWebhookCallback() {
  if (!webhookCallbackDisplay.value) return
  navigator.clipboard.writeText(webhookCallbackDisplay.value).then(() => {
    proxy.$modal.msgSuccess('回调地址已复制')
  }).catch(() => {
    proxy.$modal.msgError('复制失败，请手动选择复制')
  })
}

function getList() {
  loading.value = true
  listReviewProject(queryParams.value).then(response => {
    projectList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => { loading.value = false })
}

function loadOptions() {
  return getReviewProjectOptions().then(response => {
    Object.assign(options, response.data || {})
  })
}

function reset() {
  form.value = {
    projectId: undefined, projectName: undefined, provider: 'GITHUB', repositoryUrl: undefined,
    repositoryOwner: undefined, repositoryName: undefined, defaultBranch: undefined,
    prReviewEnabled: '0', prTargetBranches: [], businessSystemId: undefined, deptId: undefined,
    ownerUserId: undefined, credentialId: undefined, modelId: undefined, templateId: undefined,
    primaryStack: 'FULLSTACK', reviewMode: 'OCR_ENGINE', engineCode: 'OPEN_CODE_REVIEW',
    status: '1', lastBranchSyncStatus: 'UNSYNCED',
    lastBranchSyncMessage: undefined, lastBranchSyncTime: undefined, remark: undefined,
    webhookSecret: undefined, webhookSecretConfigured: false, webhookCallbackUrl: undefined,
    lastWebhookTime: undefined, lastWebhookResult: undefined,
    scopeExcludePatterns: undefined, scopeIncludeTests: 'N', scopeReportExisting: 'N', scopeExpandEnabled: 'Y'
  }
  activeTab.value = 'basic'
  repositoryInfoLoaded.value = false
  loadedRepositorySignature.value = ''
  originalRepositorySignature.value = ''
  availableBranches.value = []
  recommendedBranches.value = []
  branchCount.value = undefined
  branchDialogOpen.value = false
  branchSearch.value = ''
  advancedSections.value = []
  showAllTemplates.value = false
  proxy.resetForm('projectRef')
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function cancel() { open.value = false }
function scrollProjectFormToTop() {
  window.setTimeout(() => {
    const formElement = proxy.$refs.projectRef?.$el
    if (formElement) formElement.scrollTop = 0
  }, 0)
}
function handleAdd() {
  reset()
  open.value = true
  title.value = '新增 GitHub 项目'
}

function handleUpdate(row) {
  reset()
  getReviewProject(row.projectId).then(response => {
    const project = response.data || {}
    project.prTargetBranches = splitBranches(project.prTargetBranches)
    project.reviewMode = normalizeReviewMode(project.reviewMode)
    project.primaryStack = project.primaryStack || 'FULLSTACK'
    project.scopeIncludeTests = project.scopeIncludeTests || 'N'
    project.scopeReportExisting = project.scopeReportExisting || 'N'
    project.scopeExpandEnabled = project.scopeExpandEnabled || 'Y'
    if (project.reviewMode === 'OCR_ENGINE') {
      project.modelId = undefined
      project.templateId = undefined
      project.engineCode = project.engineCode || 'OPEN_CODE_REVIEW'
    } else {
      project.engineCode = undefined
    }
    form.value = project
    availableBranches.value = [...project.prTargetBranches]
    originalRepositorySignature.value = repositorySignature(project)
    open.value = true
    title.value = '修改 GitHub 项目'
  })
}

function normalizeReviewMode(mode) {
  if (mode === 'OCR_PR_DIFF') return 'OCR_ENGINE'
  return mode || 'OCR_ENGINE'
}

function handleReviewModeChange(mode) {
  if (mode === 'LLM_DIRECT') {
    form.value.engineCode = undefined
    suggestTemplateByStack()
  } else {
    form.value.modelId = undefined
    form.value.templateId = undefined
    form.value.engineCode = form.value.engineCode || 'OPEN_CODE_REVIEW'
  }
  proxy.$refs.projectRef?.clearValidate(['modelId', 'templateId', 'engineCode'])
}

function handlePrimaryStackChange() {
  if (form.value.reviewMode === 'LLM_DIRECT') {
    suggestTemplateByStack()
  }
}

function suggestTemplateByStack() {
  if (form.value.templateId) return
  const preferred = (options.templates || []).find(item => item.techStack === form.value.primaryStack)
  if (preferred) {
    form.value.templateId = preferred.id
  } else if ((options.templates || []).length === 1) {
    form.value.templateId = options.templates[0].id
  }
}

function formatTemplateLabel(item) {
  const stack = dictLabel(review_tech_stack.value, item.techStack)
  const version = item.versionNo != null ? `v${item.versionNo}` : 'v1'
  return `${item.label}（${stack} · ${version}）`
}

function dictLabel(optionsList, value) {
  const hit = (optionsList || []).find(item => item.value === value)
  return hit ? hit.label : (value || '通用')
}

function prevTab() {
  const index = tabOrder.indexOf(activeTab.value)
  if (index > 0) activeTab.value = tabOrder[index - 1]
}

function nextTab() {
  const index = tabOrder.indexOf(activeTab.value)
  if (index < tabOrder.length - 1) activeTab.value = tabOrder[index + 1]
}

function openModelService() {
  const route = router.resolve({ path: '/model-service/ai-model-config' })
  window.open(route.href, '_blank', 'noopener,noreferrer')
}

function openTemplateManagement() {
  const route = router.resolve({ path: '/review/template' })
  window.open(route.href, '_blank', 'noopener,noreferrer')
}

function openEnginePage() {
  const route = router.resolve({ path: '/model-service/engine' })
  window.open(route.href, '_blank', 'noopener,noreferrer')
}

function handleSystemChange(systemId) {
  const system = options.businessSystems.find(item => item.id === systemId)
  form.value.deptId = system?.deptId
  if (!availableOwners.value.some(item => item.id === form.value.ownerUserId)) form.value.ownerUserId = undefined
}

function invalidateRepositoryInfo() {
  repositoryInfoLoaded.value = false
  loadedRepositorySignature.value = ''
  availableBranches.value = []
  recommendedBranches.value = []
  branchCount.value = undefined
  form.value.repositoryOwner = undefined
  form.value.repositoryName = undefined
  form.value.defaultBranch = undefined
  form.value.prTargetBranches = []
  form.value.lastBranchSyncStatus = 'UNSYNCED'
  form.value.lastBranchSyncMessage = undefined
  form.value.lastBranchSyncTime = undefined
}

function handleReadRepositoryInfo() {
  if (!form.value.repositoryUrl) {
    proxy.$modal.msgWarning('请先填写 GitHub 仓库地址')
    return
  }
  if (!form.value.credentialId) {
    proxy.$modal.msgWarning('请先选择 GitHub 访问凭据')
    return
  }
  repositoryReading.value = true
  readReviewProjectRepositoryInfo({
    projectId: form.value.projectId,
    repositoryUrl: form.value.repositoryUrl,
    credentialId: form.value.credentialId
  }).then(response => {
    const result = response.data || {}
    form.value.lastBranchSyncStatus = result.success ? 'SUCCESS' : 'FAILED'
    form.value.lastBranchSyncMessage = result.message
    form.value.lastBranchSyncTime = result.syncedAt
    if (!result.success) {
      repositoryInfoLoaded.value = false
      proxy.$modal.msgError(result.message || '仓库信息读取失败')
      return
    }
    const branches = result.branches || []
    const selected = (form.value.prTargetBranches || []).filter(branch => branches.includes(branch))
    form.value.repositoryUrl = result.repositoryUrl
    form.value.repositoryOwner = result.repositoryOwner
    form.value.repositoryName = result.repositoryName
    form.value.defaultBranch = result.defaultBranch
    form.value.prTargetBranches = selected.length ? selected : [...(result.recommendedTargetBranches || [])]
    availableBranches.value = branches
    recommendedBranches.value = result.recommendedTargetBranches || []
    branchCount.value = branches.length
    repositoryInfoLoaded.value = true
    loadedRepositorySignature.value = repositorySignature(form.value)
    proxy.$refs.projectRef?.clearValidate(['repositoryUrl', 'credentialId', 'prTargetBranches'])
    proxy.$modal.msgSuccess(`仓库信息读取成功，共 ${branches.length} 个分支`)
  }).finally(() => { repositoryReading.value = false })
}

function handlePrReviewChange(value) {
  if (value === '0' && (!form.value.prTargetBranches || form.value.prTargetBranches.length === 0)) {
    form.value.prTargetBranches = [...recommendedBranches.value]
  }
  proxy.$refs.projectRef?.validateField('prTargetBranches').catch(() => {})
}

function openCredentialManagement() {
  const route = router.resolve({ path: '/review/credential' })
  window.open(route.href, '_blank', 'noopener,noreferrer')
}

function refreshCredentialOptions() {
  loadOptions().then(() => proxy.$modal.msgSuccess('凭据列表已刷新'))
}

function submitForm() {
  proxy.$refs.projectRef.validate(valid => {
    if (!valid) {
      if (!form.value.projectName || !form.value.businessSystemId || !form.value.ownerUserId) {
        activeTab.value = 'basic'
      } else if (!form.value.repositoryUrl || !form.value.credentialId
        || (form.value.prReviewEnabled === '0' && !(form.value.prTargetBranches || []).length)) {
        activeTab.value = 'repository'
      } else {
        activeTab.value = 'execution'
      }
      return
    }
    const repositoryChanged = currentRepositorySignature.value !== originalRepositorySignature.value
    if ((!form.value.projectId || repositoryChanged)
      && loadedRepositorySignature.value !== currentRepositorySignature.value) {
      activeTab.value = 'repository'
      proxy.$modal.msgWarning('请先读取仓库信息，再保存项目')
      return
    }
    const payload = {
      ...form.value,
      reviewMode: normalizeReviewMode(form.value.reviewMode),
      prTargetBranches: (form.value.prTargetBranches || []).join(',')
    }
    if (payload.reviewMode === 'LLM_DIRECT') {
      payload.engineCode = null
    } else {
      payload.modelId = null
      payload.templateId = null
      payload.engineCode = payload.engineCode || 'OPEN_CODE_REVIEW'
    }
    submitting.value = true
    const action = form.value.projectId ? updateReviewProject(payload) : addReviewProject(payload)
    action.then(() => {
      proxy.$modal.msgSuccess(form.value.projectId ? '修改成功' : '新增成功')
      open.value = false
      getList()
    }).finally(() => { submitting.value = false })
  })
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除项目“' + row.projectName + '”？').then(() => delReviewProject(row.projectId)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function handleStatusChange(row) {
  const targetStatus = row.status === '0' ? '1' : '0'
  const label = targetStatus === '0' ? '启用' : '停用'
  proxy.$modal.confirm('是否确认' + label + '项目“' + row.projectName + '”？').then(() => {
    return changeReviewProjectStatus(row.projectId, targetStatus)
  }).then(() => {
    row.status = targetStatus
    proxy.$modal.msgSuccess('项目已' + label)
  }).catch(() => {})
}

function handleTest(row) {
  testingId.value = row.projectId
  testReviewProject(row.projectId).then(response => {
    const result = response.data
    if (result.success) proxy.$modal.msgSuccess(result.message)
    else proxy.$modal.msgError(result.message)
    getList()
  }).finally(() => { testingId.value = undefined })
}

function repositorySignature(project) {
  return `${(project.repositoryUrl || '').trim()}|${project.credentialId || ''}`
}

function splitBranches(value) {
  if (!value) return []
  return value.split(',').map(item => item.trim()).filter(Boolean)
}

function formatTargetBranches(value) {
  const branches = splitBranches(value)
  return branches.length ? branches.join('、') : '未选择目标分支'
}

function checkStatusText(status) {
  return { SUCCESS: '连接正常', FAILED: '连接失败', UNTESTED: '未检测' }[status] || '未检测'
}

function checkTagType(status) {
  return { SUCCESS: 'success', FAILED: 'danger', UNTESTED: 'info' }[status] || 'info'
}

function syncStatusText(status) {
  return { SUCCESS: '同步成功', FAILED: '同步失败', UNSYNCED: '未同步' }[status] || '未同步'
}

function syncTagType(status) {
  return { SUCCESS: 'success', FAILED: 'danger', UNSYNCED: 'info' }[status] || 'info'
}

Promise.all([loadOptions(), getList()])
</script>

<style scoped>
.text-muted { margin-top: 4px; color: var(--el-text-color-secondary); font-size: 12px; }
.text-ellipsis { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mr8 { margin-right: 8px; }
.mb12 { margin-bottom: 12px; }

.project-form {
  max-height: calc(90vh - 180px);
  overflow-y: auto;
  padding-right: 8px;
}
.project-form :deep(.el-form-item) {
  margin-bottom: 16px;
}
.project-form :deep(.el-form-item__content) {
  min-width: 0;
}
.project-form :deep(.el-form-item__content > .el-input),
.project-form :deep(.el-form-item__content > .el-select),
.project-form :deep(.el-form-item__content > .el-textarea),
.project-form :deep(.form-control-block > .el-input),
.project-form :deep(.form-control-block > .el-select),
.project-form :deep(.form-control-block > .el-textarea) {
  width: 100%;
}

.project-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
.project-tabs :deep(.el-tabs__content) { min-height: 360px; }

.form-control-block {
  width: 100%;
  min-width: 0;
}

.field-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}
.field-actions--split {
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px 16px;
}
.field-actions__links {
  flex: 0 0 auto;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.repository-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 24px;
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
}
.repository-summary div { display: flex; min-width: 0; }
.repository-summary span { flex: 0 0 76px; color: var(--el-text-color-secondary); }
.repository-summary strong {
  overflow: hidden;
  color: var(--el-text-color-regular);
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inline-tip {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 20px;
}
.inline-tip--inline {
  margin-top: 0;
  flex: 1;
  min-width: 200px;
}
.inline-tip.is-error {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--el-color-danger);
}

.advanced-settings {
  margin: 0 0 4px 110px;
  border-top: 0;
}
.advanced-settings :deep(.el-collapse-item__header) {
  height: 36px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.advanced-settings :deep(.el-collapse-item__wrap) { border-bottom: 0; }
.advanced-settings :deep(.el-collapse-item__content) { padding-bottom: 2px; }
.advanced-settings :deep(.el-form-item) { margin-bottom: 12px; }

@media (max-width: 900px) {
  .field-actions--split { flex-direction: column; }
  .field-actions__links { white-space: normal; }
  .repository-summary { grid-template-columns: 1fr; }
}
</style>
