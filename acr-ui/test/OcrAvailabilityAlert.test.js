import test from 'node:test'
import assert from 'node:assert/strict'
import { createSSRApp, h } from 'vue'
import { renderToString } from '@vue/server-renderer'
import OcrAvailabilityAlert from '../src/components/OcrAvailabilityAlert/index.js'

test('renders non-blocking OCR unavailable warning for the project form', async () => {
  const app = createSSRApp({
    render: () => h(OcrAvailabilityAlert, {
      availability: {
        available: false,
        executable: 'ocr',
        message: '未检测到 OCR 引擎（命令：ocr），请安装 open-code-review 或检查 ACR_OCR_EXECUTABLE 配置'
      }
    })
  })

  const html = await renderToString(app)

  assert.match(html, /OCR 引擎不可用/)
  assert.match(html, /未检测到 OCR 引擎（命令：ocr）/)
  assert.match(html, /ACR_OCR_EXECUTABLE/)
  assert.match(html, /不阻断项目保存/)
})

test('renders available OCR version details for an OCR project', async () => {
  const app = createSSRApp({
    render: () => h(OcrAvailabilityAlert, {
      availability: {
        available: true,
        executable: '/opt/bin/ocr',
        version: 'open-code-review v1.2.3',
        detectedAt: '2026-08-23T12:00:00Z'
      }
    })
  })

  const html = await renderToString(app)

  assert.match(html, /OCR 引擎可用/)
  assert.match(html, /open-code-review v1.2.3/)
  assert.match(html, /\/opt\/bin\/ocr/)
})
