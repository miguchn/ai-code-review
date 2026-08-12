/**
 * ECharts 按需引入封装：工作台等页面统一从这里导入，避免全量打包。
 * 需要新图表类型/组件时在下方 use 列表追加。
 */
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([BarChart, LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

export default echarts
