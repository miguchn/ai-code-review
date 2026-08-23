export const TOUR_VERSION = 'v1'

export const TOUR_IDS = {
  ADMIN_FIRST_PROJECT: 'admin-first-project',
  DEVELOPER_FIRST_ISSUE: 'developer-first-issue',
  MANAGER_FIRST_INSIGHT: 'manager-first-insight'
}

export const TOUR_MANIFEST = [
  {
    id: TOUR_IDS.ADMIN_FIRST_PROJECT,
    title: '管理员：接入第一个项目',
    description: '从平台凭据到 Webhook，再确认首个审查任务。',
    steps: [
      {
        id: 'admin-workbench-entry',
        route: '/index',
        selector: '[data-tour="workbench-quick-project"]',
        title: '从工作台开始',
        content: '“接入项目”会直达代码项目配置，是管理员最短的开始路径。'
      },
      {
        id: 'admin-credential-add',
        route: '/project-access/credential',
        selector: '[data-tour="credential-add"]',
        title: '先准备访问凭据',
        content: '新增对应 Git 平台的最小权限 Token，系统不会回显已保存的明文。'
      },
      {
        id: 'admin-credential-list',
        route: '/project-access/credential',
        selector: '[data-tour="credential-list"]',
        title: '确认连接可用',
        content: '在凭据列表执行“检测”，连接正常后再把它绑定到代码项目。'
      },
      {
        id: 'admin-project-add',
        route: '/project-access/project',
        selector: '[data-tour="project-add"]',
        title: '新建代码项目',
        content: '点击“新增”，按向导填写归属、仓库、分支和审查范围。'
      },
      {
        id: 'admin-project-wizard',
        route: '/project-access/project',
        selector: '[data-tour="project-wizard-tabs"]',
        prepare: { click: '[data-tour="project-add"]' },
        title: '沿向导完成接入',
        content: '依次完成基础信息、仓库与分支，并确认需要触发的审查类型。'
      },
      {
        id: 'admin-project-webhook-tab',
        route: '/project-access/project',
        selector: '[data-tour="project-webhook-tab"]',
        title: '进入 Webhook 配置',
        content: '切到 Webhook 页，把平台生成的回调地址和 Secret 配到代码仓库。'
      },
      {
        id: 'admin-project-webhook-form',
        route: '/project-access/project',
        selector: '[data-tour="project-webhook-form"]',
        prepare: { click: '[data-tour="project-webhook-tab"]' },
        title: '核对回调与密钥',
        content: '回调地址、事件类型和 Secret 必须与 Git 平台配置完全一致。'
      },
      {
        id: 'admin-review-task',
        route: '/review/task',
        selector: '[data-tour="review-task-table"]',
        title: '验证首次触发',
        content: '在 Git 平台创建或更新合并请求、或推送配置分支后，到这里确认任务已进入队列。'
      }
    ]
  },
  {
    id: TOUR_IDS.DEVELOPER_FIRST_ISSUE,
    title: '开发者：处理第一个审查问题',
    description: '从工作台待办进入台账，完成确认或其他处置。',
    steps: [
      {
        id: 'developer-workbench-todos',
        route: '/index',
        selector: '[data-tour="workbench-todos"]',
        title: '先看今日待办',
        content: '工作台把待确认、待修复、待复核和失败事项聚合为可直达的卡片。'
      },
      {
        id: 'developer-awaiting-confirm',
        route: '/index',
        selector: '[data-tour="workbench-awaiting-confirm"]',
        title: '打开待确认问题',
        content: '点击“待确认问题”会带着状态和归属筛选进入问题台账。'
      },
      {
        id: 'developer-issue-stats',
        route: { path: '/review/issue', query: { status: 'AWAITING_CONFIRM', origin: 'NEW' } },
        selector: '[data-tour="issue-stats"]',
        title: '确认当前治理阶段',
        content: '状态总览可快速切换待确认、待修复、疑似修复和累计关闭问题。'
      },
      {
        id: 'developer-issue-table',
        route: { path: '/review/issue', query: { status: 'AWAITING_CONFIRM', origin: 'NEW' } },
        selector: '[data-tour="issue-table"]',
        title: '找到需要处理的问题',
        content: '优先结合严重度、阶段、责任人和代码位置判断处置顺序。'
      },
      {
        id: 'developer-issue-detail',
        route: { path: '/review/issue', query: { status: 'AWAITING_CONFIRM', origin: 'NEW' } },
        selector: '[data-tour="issue-detail"]',
        prepare: { click: '[data-tour="issue-detail-trigger"]' },
        title: '阅读问题证据',
        content: '详情汇总上下文、定位、建议和审查轨迹，先核实再改变状态。'
      },
      {
        id: 'developer-issue-lifecycle',
        route: { path: '/review/issue', query: { status: 'AWAITING_CONFIRM', origin: 'NEW' } },
        selector: '[data-tour="issue-lifecycle"]',
        title: '看懂生命周期',
        content: '问题会沿发现、确认、待修复、疑似修复到关闭持续留痕。'
      },
      {
        id: 'developer-issue-actions',
        route: { path: '/review/issue', query: { status: 'AWAITING_CONFIRM', origin: 'NEW' } },
        selector: '[data-tour="issue-actions"]',
        title: '选择正确处置',
        content: '需要整改就确认或转派，不需治理则填写原因后关闭、忽略或标记误报。'
      }
    ]
  },
  {
    id: TOUR_IDS.MANAGER_FIRST_INSIGHT,
    title: '管理者：看懂工作台与数据洞察',
    description: '从当日风险信号下钻到趋势、指标与项目矩阵。',
    steps: [
      {
        id: 'manager-workbench-summary',
        route: '/index',
        selector: '[data-tour="workbench-summary"]',
        title: '确认数据范围',
        content: '先看可见项目数和最近任务时间，避免在错误范围内解读指标。'
      },
      {
        id: 'manager-workbench-todos',
        route: '/index',
        selector: '[data-tour="workbench-todos"]',
        title: '识别当日风险',
        content: '待办卡片优先暴露高风险结论、逾期问题、失败任务和投递异常。'
      },
      {
        id: 'manager-workbench-trend',
        route: '/index',
        selector: '[data-tour="workbench-trend"]',
        title: '观察短期趋势',
        content: '项目风险趋势按审查完成日期展示近期通过、警告、阻断和失败变化。'
      },
      {
        id: 'manager-workbench-recent',
        route: '/index',
        selector: '[data-tour="workbench-recent"]',
        title: '下钻最近异常',
        content: '最近动态提供任务级入口，用于核对风险结论发生在哪个项目和变更。'
      },
      {
        id: 'manager-insight-filters',
        route: '/insight/overview',
        selector: '[data-tour="insight-filters"]',
        title: '统一分析范围',
        content: '用时间范围和业务系统筛选固定统计口径，再比较不同周期。'
      },
      {
        id: 'manager-insight-kpis',
        route: '/insight/overview',
        selector: '[data-tour="insight-kpis"]',
        title: '先读四个核心指标',
        content: '覆盖率、成功率、P95 时延和未处置重点问题共同反映质量与运行健康。'
      },
      {
        id: 'manager-insight-charts',
        route: '/insight/overview',
        selector: '[data-tour="insight-charts"]',
        title: '结合趋势定位变化',
        content: '任务、问题类别和交付健康图表用于判断指标变化来自哪里。'
      },
      {
        id: 'manager-project-matrix',
        route: '/insight/project',
        selector: '[data-tour="insight-project-table"]',
        title: '下钻到项目',
        content: '项目矩阵用任务量、成功率、重点问题和处置率定位需要关注的项目。'
      }
    ]
  }
]

export function findTour(id) {
  return TOUR_MANIFEST.find(tour => tour.id === id) || null
}

export function completionStorageKey(userId) {
  return `acr:tour:${TOUR_VERSION}:completed:${userId}`
}

export function pickAutoTourId({ roles = [], permissions = [] } = {}) {
  const normalizedRoles = roles.map(role => String(role).toLowerCase())
  if (normalizedRoles.includes('admin') || permissions.includes('*:*:*') || permissions.includes('review:project:add')) {
    return TOUR_IDS.ADMIN_FIRST_PROJECT
  }
  if (permissions.some(permission => String(permission).startsWith('review:insight:'))) {
    return TOUR_IDS.MANAGER_FIRST_INSIGHT
  }
  return TOUR_IDS.DEVELOPER_FIRST_ISSUE
}
