import { request } from '@/shared/api/http'

export async function downloadJsonExport(): Promise<void> {
  const response = await request<Blob>({ method: 'get', url: '/export/json', responseType: 'blob' })
  const blob = response.data instanceof Blob ? response.data : new Blob([response.data])
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `interview-record-export-${new Date().toISOString().slice(0, 10)}.json`
  anchor.click()
  URL.revokeObjectURL(url)
}
