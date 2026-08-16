/** 投递趋势柱状图的纯计算逻辑（无渲染依赖，便于单元测试）。 */

export interface TrendPoint {
  date: string
  applicationCount: number
  interviewRoundCount: number
}

export interface TrendBar {
  /** 柱的 x 坐标 */
  x: number
  /** 短日期标签，例如 08/13 */
  label: string
  applicationHeight: number
  applicationY: number
  roundHeight: number
  roundY: number
  tooltip: string
}

export interface TrendChart {
  bars: TrendBar[]
  maxValue: number
  gridValues: number[]
}

export function shortDateLabel(date: string): string {
  return date.length >= 10 ? date.substring(5).replace('-', '/') : date
}

/**
 * 把趋势点换算成 SVG 柱状图坐标：
 * - 双系列（投递岗位、面试轮次）并排，各自按最大值缩放；
 * - 返回绘图区尺寸与网格线取值，模板只负责 <rect>/<text>。
 */
export function buildTrendChart(points: TrendPoint[], width: number, height: number): TrendChart {
  const padding = { top: 8, right: 8, bottom: 26, left: 30 }
  const plotWidth = Math.max(0, width - padding.left - padding.right)
  const plotHeight = Math.max(0, height - padding.top - padding.bottom)
  const maxValue = Math.max(1, ...points.map((point) =>
    Math.max(point.applicationCount, point.interviewRoundCount)))
  const gridValues = [0, Math.ceil(maxValue / 2), maxValue]
  const slot = points.length === 0 ? 0 : plotWidth / points.length
  const barWidth = Math.max(2, Math.min(14, slot / 3.4))
  const scale = (value: number) => Math.round((value / maxValue) * plotHeight)

  const bars = points.map((point, index) => {
    const center = padding.left + slot * index + slot / 2
    const applicationHeight = scale(point.applicationCount)
    const roundHeight = scale(point.interviewRoundCount)
    return {
      x: center - barWidth - 1,
      label: shortDateLabel(point.date),
      applicationHeight,
      applicationY: padding.top + plotHeight - applicationHeight,
      roundHeight,
      roundY: padding.top + plotHeight - roundHeight,
      tooltip: `${shortDateLabel(point.date)}：投递 ${point.applicationCount}，面试 ${point.interviewRoundCount}`,
    }
  })

  return { bars, maxValue, gridValues }
}
