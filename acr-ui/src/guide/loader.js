// markdown 原文：按 docId 对应文件懒加载（glob 只注册路径，文件缺失不影响构建）
const mdModules = import.meta.glob('./content/**/*.md', { query: '?raw', import: 'default' })
// 指引截图：构建期解析为带 hash 的 URL，markdown 中以文件名引用
const assetModules = import.meta.glob('./assets/*.{png,jpg,jpeg,svg,webp}', { eager: true, import: 'default' })

const assetUrls = {}
for (const [path, url] of Object.entries(assetModules)) {
  assetUrls[path.split('/').pop()] = url
}

/** 按文件名取图片 URL；不存在返回空串 */
export function getAssetUrl(fileName) {
  return assetUrls[fileName] || ''
}

/**
 * 加载 markdown 原文。
 * @param {string} file manifest 中的相对路径，如 'platforms/github.md'
 * @returns {Promise<string|null>} 原文；缺失或加载失败返回 null
 */
export async function loadDocContent(file) {
  const loader = mdModules[`./content/${file}`]
  if (!loader) return null
  try {
    return await loader()
  } catch {
    return null
  }
}
