import { describe, expect, it } from 'vitest'
import { applyDark, applyTheme, themeOptions } from './theme'

describe('theme application', () => {
  it('persists the selected theme as a document token', () => {
    applyTheme('FOREST_TEAL')
    expect(document.documentElement.dataset.theme).toBe('FOREST_TEAL')
    applyTheme('GRAPHITE_CORAL')
    expect(document.documentElement.dataset.theme).toBe('GRAPHITE_CORAL')
  })

  it('applies dark mode independently from the accent theme', () => {
    applyDark(true)
    expect(document.documentElement.dataset.dark).toBe('true')
    applyDark(false)
    expect(document.documentElement.dataset.dark).toBeUndefined()
  })

  it('keeps the four product themes available to both settings and the quick picker', () => {
    expect(themeOptions.map((option) => option.value)).toEqual([
      'INDIGO', 'FOREST_TEAL', 'WARM_APRICOT', 'GRAPHITE_CORAL',
    ])
    expect(themeOptions.at(-1)?.value).toBe('GRAPHITE_CORAL')
  })
})
