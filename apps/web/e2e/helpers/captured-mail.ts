import { readFile } from 'node:fs/promises'

type CapturedMailType = 'VERIFY_EMAIL' | 'RESET_PASSWORD'

interface CapturedMail {
  recipient: string
  type: CapturedMailType
  url: string
  createdAt: string
}

const pollIntervalMs = 100
const timeoutMs = 10_000

function mailboxPath(): string {
  const path = process.env.E2E_MAILBOX_PATH
  if (!path) throw new Error('E2E_MAILBOX_PATH must be set for account lifecycle tests')
  return path
}

async function readMessages(): Promise<CapturedMail[]> {
  try {
    const content = await readFile(mailboxPath(), 'utf8')
    return content.split('\n').filter(Boolean).flatMap((line) => {
      try { return [JSON.parse(line) as CapturedMail] }
      catch { return [] }
    })
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') return []
    throw error
  }
}

export async function waitForCapturedEmailLink(recipient: string, type: CapturedMailType): Promise<string> {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const matches = (await readMessages()).filter((message) => message.recipient === recipient && message.type === type)
    const newest = matches.at(-1)
    if (newest) return newest.url
    await new Promise((resolve) => setTimeout(resolve, pollIntervalMs))
  }
  throw new Error(`No captured ${type} message arrived for ${recipient} within ${timeoutMs}ms`)
}
