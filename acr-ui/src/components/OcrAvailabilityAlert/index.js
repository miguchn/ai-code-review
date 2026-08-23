import { defineComponent, h } from 'vue'
import { ElAlert } from 'element-plus'

const INSTALL_GUIDANCE = '请安装 open-code-review 或检查 ACR_OCR_EXECUTABLE 配置。保存项目不受影响。'

export function buildOcrAvailabilityPresentation({ availability, loading, error }) {
  if (loading) {
    return {
      type: 'info',
      title: '正在检测 OCR 引擎可用性',
      description: '检测结果仅用于前置提示，不阻断项目保存。'
    }
  }
  if (error) {
    return {
      type: 'warning',
      title: '暂时无法获取 OCR 引擎状态',
      description: `探针接口调用失败。${INSTALL_GUIDANCE}`
    }
  }
  if (!availability) return null
  if (availability.available) {
    const detail = [
      availability.executable ? `命令：${availability.executable}` : '',
      availability.version ? `版本：${availability.version}` : '',
      availability.detectedAt ? `探测时间：${formatDetectedAt(availability.detectedAt)}` : ''
    ].filter(Boolean).join('；')
    return {
      type: 'success',
      title: 'OCR 引擎可用',
      description: detail || '版本探测成功。'
    }
  }
  return {
    type: 'warning',
    title: 'OCR 引擎不可用',
    description: `${availability.message || INSTALL_GUIDANCE}；该提示不阻断项目保存。`
  }
}

function formatDetectedAt(value) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('zh-CN', { hour12: false })
}

export default defineComponent({
  name: 'OcrAvailabilityAlert',
  props: {
    availability: { type: Object, default: null },
    loading: { type: Boolean, default: false },
    error: { type: String, default: '' }
  },
  setup(props) {
    return () => {
      const presentation = buildOcrAvailabilityPresentation(props)
      if (!presentation) return null
      return h(ElAlert, {
        type: presentation.type,
        title: presentation.title,
        description: presentation.description,
        closable: false,
        showIcon: true
      })
    }
  }
})
