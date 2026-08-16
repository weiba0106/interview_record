import { describe, expect, it } from 'vitest'
import { buildTrendChart, shortDateLabel } from './trend-chart'

describe('buildTrendChart', () => {
  it('scales both series against the shared maximum', () => {
    const chart = buildTrendChart([
      { date: '2026-08-13', applicationCount: 2, interviewRoundCount: 1 },
      { date: '2026-08-14', applicationCount: 4, interviewRoundCount: 3 },
    ], 400, 200)

    expect(chart.maxValue).toBe(4)
    // 绘图区高度 = 200 - 8 - 26 = 166
    const plotHeight = 166
    expect(chart.bars[0]!.applicationHeight).toBe(Math.round((2 / 4) * plotHeight))
    expect(chart.bars[1]!.applicationHeight).toBe(plotHeight)
    expect(chart.bars[1]!.roundHeight).toBe(Math.round((3 / 4) * plotHeight))
    expect(chart.bars[1]!.applicationY + chart.bars[1]!.applicationHeight).toBe(8 + plotHeight)
  })

  it('returns empty bars and zero grid values for empty input', () => {
    const chart = buildTrendChart([], 400, 200)

    expect(chart.bars).toEqual([])
    expect(chart.maxValue).toBe(1)
  })

  it('splits the plot width evenly between bars with x labels in order', () => {
    const chart = buildTrendChart([
      { date: '2026-08-13', applicationCount: 1, interviewRoundCount: 0 },
      { date: '2026-08-14', applicationCount: 0, interviewRoundCount: 2 },
    ], 400, 200)

    expect(chart.bars.map((bar) => bar.label)).toEqual(['08/13', '08/14'])
    expect(chart.bars[1]!.x).toBeGreaterThan(chart.bars[0]!.x)
    expect(chart.bars[0]!.tooltip).toContain('投递 1，面试 0')
  })

  it('formats date labels as MM/DD', () => {
    expect(shortDateLabel('2026-08-13')).toBe('08/13')
    expect(shortDateLabel('2026-1-2')).toBe('2026-1-2')
  })
})
