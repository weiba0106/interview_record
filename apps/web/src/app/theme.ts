import type { CurrentUser } from '@/shared/auth/auth.types'

export type ThemeName = CurrentUser['theme']

/** 主题元数据：同时供快捷切换和设置页的真实颜色预览使用 */
export interface ThemePalette {
  /** 侧边栏底色 */
  sidebar: string
  /** 品牌强调色 */
  primary: string
  /** 实底按钮色（与白色文字满足 AA） */
  primaryStrong: string
  /** 页面背景 */
  background: string
  /** 卡片背景 */
  card: string
  /** 边框色 */
  border: string
}

export const themeOptions: Array<{
  value: ThemeName
  label: string
  description: string
  palette: ThemePalette
}> = [
  {
    value: 'INDIGO',
    label: '原始靛蓝',
    description: '清晰、专注的经典工作台',
    palette: { sidebar: '#202747', primary: '#4f5bd5', primaryStrong: '#4f5bd5', background: '#f5f7ff', card: '#ffffff', border: '#dfe4f5' },
  },
  {
    value: 'FOREST_TEAL',
    label: '森林青绿',
    description: '沉稳、自然的低干扰配色',
    palette: { sidebar: '#163f3b', primary: '#2f8578', primaryStrong: '#26746a', background: '#f3f8f6', card: '#ffffff', border: '#d9e6e1' },
  },
  {
    value: 'WARM_APRICOT',
    label: '暖杏棕',
    description: '温暖、柔和的记录空间',
    palette: { sidebar: '#594234', primary: '#c86e3e', primaryStrong: '#b25e2f', background: '#fbf7f2', card: '#ffffff', border: '#eadccc' },
  },
  {
    value: 'GRAPHITE_CORAL',
    label: '石墨珊瑚',
    description: '默认主题，深色与珊瑚强调色',
    palette: { sidebar: '#242725', primary: '#ef5e58', primaryStrong: '#d2403a', background: '#f6f7f6', card: '#ffffff', border: '#e2e5e2' },
  },
]

export function themeLabel(value: ThemeName | undefined | null): string {
  return themeOptions.find((option) => option.value === value)?.label ?? '石墨珊瑚'
}

export function applyTheme(theme: ThemeName | undefined | null): void {
  document.documentElement.dataset.theme = theme || 'GRAPHITE_CORAL'
}
