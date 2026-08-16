import { request } from '@/shared/api/http'

export interface ExportCreated {
  token: string
  fileName: string
  expiresAt: string
}

/** 生成一次性导出（csv=CSV ZIP，json=JSON 完整备份），返回 30 分钟有效的下载令牌。 */
export async function createExport(kind: 'csv' | 'json'): Promise<ExportCreated> {
  return (await request<ExportCreated>({ method: 'post', url: `/export/${kind}` })).data
}

/** 通过一次性令牌下载并触发浏览器保存；令牌使用一次后失效。 */
export async function downloadExport(token: string, fileName: string): Promise<void> {
  const response = await request<Blob>({ method: 'get', url: `/export/download/${token}`, responseType: 'blob' })
  const blob = response.data instanceof Blob ? response.data : new Blob([response.data])
  if (typeof URL.createObjectURL !== 'function') return
  const url = URL.createObjectURL(blob)
  try {
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = fileName
    anchor.click()
  } finally {
    URL.revokeObjectURL(url)
  }
}
