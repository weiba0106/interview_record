import { randomUUID } from 'node:crypto'
import { expect, test } from '@playwright/test'
import { waitForCapturedEmailLink } from './helpers/captured-mail'

const initialPassword = 'Password123'
const replacementPassword = 'NewPassword123'

async function deleteAccount(page: import('@playwright/test').Page, password: string): Promise<void> {
  await page.goto('/app/settings')
  await page.getByRole('button', { name: '删除账号' }).click()
  await page.getByLabel('输入密码确认').fill(password)
  await page.getByRole('button', { name: '永久删除' }).click()
  await expect(page).toHaveURL(/\/login$/)
}

async function loginForCleanup(page: import('@playwright/test').Page, email: string, password: string): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('邮箱').fill(email)
  await page.getByLabel('密码').fill(password)
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/app$/)
}

test('register, verify, login, update preferences, reset, and delete', async ({ page, browser }) => {
  const email = `e2e-${randomUUID()}@example.test`
  const otherEmail = `e2e-other-${randomUUID()}@example.test`
  let primaryRegistered = false
  let primaryVerified = false
  let primaryLoggedIn = false
  let primaryDeleted = false
  let primaryPassword = initialPassword
  let otherRegistered = false
  let otherVerified = false
  let otherLoggedIn = false
  let otherDeleted = false
  let journeyFailed = false
  const otherContext = await browser.newContext()
  const otherPage = await otherContext.newPage()

  try {
    await page.goto('/register')
    await page.getByLabel('邮箱').fill(email)
    await page.getByLabel('密码').fill(initialPassword)
    await page.getByLabel('显示名称').fill('端到端用户')
    await page.getByRole('button', { name: '注册' }).click()
    primaryRegistered = true

    const verificationUrl = await waitForCapturedEmailLink(email, 'VERIFY_EMAIL')
    await page.goto(verificationUrl)
    await expect(page.getByText('邮箱验证成功')).toBeVisible()
    primaryVerified = true

    await page.goto('/login')
    await page.getByLabel('邮箱').fill(email)
    await page.getByLabel('密码').fill(initialPassword)
    await page.getByRole('button', { name: '登录' }).click()
    await expect(page).toHaveURL(/\/app$/)
    primaryLoggedIn = true

    await page.getByRole('link', { name: '账号设置' }).click()
    await page.getByLabel('显示名称').fill('已更新用户')
    await page.getByLabel('时区（IANA，例如 Asia/Shanghai）').fill('Asia/Tokyo')
    const theme = page.getByRole('combobox', { name: '主题' })
    await theme.click()
    await page.getByRole('option', { name: '森林青绿' }).click()
    await expect(theme).toContainText('森林青绿')
    await page.getByRole('button', { name: '保存偏好' }).click()
    await expect(page.getByText('偏好已保存')).toBeVisible()

    await page.goto('/app')
    await page.getByRole('button', { name: '退出登录' }).click()
    await expect(page).toHaveURL(/\/login$/)
    primaryLoggedIn = false

    await page.getByRole('link', { name: '忘记密码？' }).click()
    await page.getByLabel('邮箱').fill(email)
    await page.getByRole('button', { name: '发送重置邮件' }).click()
    await expect(page.getByText('如该邮箱已注册，重置邮件将很快发送。')).toBeVisible()

    const resetUrl = await waitForCapturedEmailLink(email, 'RESET_PASSWORD')
    await page.goto(resetUrl)
    await page.getByLabel('新密码').fill(replacementPassword)
    await page.getByLabel('确认新密码').fill(replacementPassword)
    await page.getByRole('button', { name: '重置密码' }).click()
    await expect(page.getByText('密码已重置，请使用新密码登录。')).toBeVisible()
    primaryPassword = replacementPassword

    await page.getByRole('link', { name: '前往登录' }).click()
    await page.getByLabel('邮箱').fill(email)
    await page.getByLabel('密码').fill(initialPassword)
    await page.getByRole('button', { name: '登录' }).click()
    await expect(page.getByRole('alert')).toBeVisible()

    await page.getByLabel('密码').fill(replacementPassword)
    await page.getByRole('button', { name: '登录' }).click()
    await expect(page).toHaveURL(/\/app$/)
    primaryLoggedIn = true

    await otherPage.goto('/register')
    await otherPage.getByLabel('邮箱').fill(otherEmail)
    await otherPage.getByLabel('密码').fill(initialPassword)
    await otherPage.getByLabel('显示名称').fill('隔离用户')
    await otherPage.getByRole('button', { name: '注册' }).click()
    otherRegistered = true
    await otherPage.goto(await waitForCapturedEmailLink(otherEmail, 'VERIFY_EMAIL'))
    await expect(otherPage.getByText('邮箱验证成功')).toBeVisible()
    otherVerified = true
    await otherPage.goto('/login')
    await otherPage.getByLabel('邮箱').fill(otherEmail)
    await otherPage.getByLabel('密码').fill(initialPassword)
    await otherPage.getByRole('button', { name: '登录' }).click()
    await expect(otherPage).toHaveURL(/\/app$/)
    otherLoggedIn = true
    await otherPage.goto('/app/settings')
    await expect(otherPage.getByLabel('显示名称')).toHaveValue('隔离用户')
    await expect(otherPage.getByText('已更新用户')).toHaveCount(0)
    await deleteAccount(otherPage, initialPassword)
    otherDeleted = true
    otherLoggedIn = false

    await deleteAccount(page, replacementPassword)
    primaryDeleted = true
    primaryLoggedIn = false
  } catch (error) {
    journeyFailed = true
    throw error
  } finally {
    const cleanupFailures: Error[] = []
    try {
      if (otherRegistered && otherVerified && !otherDeleted) {
        try {
          if (!otherLoggedIn) {
            await loginForCleanup(otherPage, otherEmail, initialPassword)
            otherLoggedIn = true
          }
          await deleteAccount(otherPage, initialPassword)
          otherDeleted = true
          otherLoggedIn = false
        } catch (error) {
          cleanupFailures.push(error instanceof Error ? error : new Error(String(error)))
        }
      }
    } finally {
      try {
        await otherContext.close()
      } catch (error) {
        cleanupFailures.push(error instanceof Error ? error : new Error(String(error)))
      }
    }

    if (primaryRegistered && primaryVerified && !primaryDeleted) {
      try {
        if (!primaryLoggedIn) {
          await loginForCleanup(page, email, primaryPassword)
          primaryLoggedIn = true
        }
        await deleteAccount(page, primaryPassword)
        primaryDeleted = true
        primaryLoggedIn = false
      } catch (error) {
        cleanupFailures.push(error instanceof Error ? error : new Error(String(error)))
      }
    }

    if ((primaryRegistered && !primaryVerified && !primaryDeleted) || (otherRegistered && !otherVerified && !otherDeleted)) {
      test.info().annotations.push({
        type: 'manual-test-data-cleanup',
        description: 'A lifecycle failure occurred before email verification, so the production authenticated deletion endpoint could not safely remove the account.',
      })
    }
    if (cleanupFailures.length > 0) {
      const description = `Could not complete ${cleanupFailures.length} authenticated E2E cleanup operation(s).`
      if (journeyFailed) test.info().annotations.push({ type: 'cleanup-failure', description })
      else throw new Error(description, { cause: cleanupFailures[0] })
    }
  }
})
