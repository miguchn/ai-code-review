import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import path from 'path'

export default function createSvgIcon(isBuild) {
  return createSvgIconsPlugin({
    // 通用 SVG 图标（命名全局唯一，与菜单 meta.icon 一致）
    iconDirs: [
      path.resolve(process.cwd(), 'src/assets/icons/svg'),
    ],
    symbolId: 'icon-[dir]-[name]',
    svgoOptions: isBuild,
  })
}
