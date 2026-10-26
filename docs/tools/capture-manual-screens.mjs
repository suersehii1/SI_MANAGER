// 사용 매뉴얼용 화면 캡처 스크립트 (Playwright, Docker 안에서 실행)
// - 실행 중인 시스템(http://host.docker.internal:3000)에 admin으로 로그인한 상태로 각 화면을 캡처한다.
// - 등록/수정 팝업은 열어서 캡처만 하고 저장하지 않는다 (DB 데이터 변경 없음).
// - 밤 스킨, 일반 사용자 화면은 /api/auth/me 응답만 브라우저 안에서 바꿔서 캡처한다 (DB 변경 없음).
// 실행: docs/tools/README.md 참고
import { chromium } from 'playwright'

const BASE = process.env.BASE_URL || 'http://host.docker.internal:3000'
const OUT = process.env.OUT_DIR || '/out'
const PROJECT_ID = process.env.PROJECT_ID || '3' // 캡처에 쓸 프로젝트 (WBS·할당 데이터가 있는 프로젝트)

const res = await fetch(`${BASE}/api/auth/login`, {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'admin1234' })
})
const { accessToken, user } = await res.json()
const browser = await chromium.launch()

// 화면 하나를 캡처한다. 실패해도 다음 화면을 계속 찍는다.
async function shot(name, path, { before, after, me } = {}) {
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  try {
    if (path !== '/login') await page.addInitScript((t) => localStorage.setItem('sipm.token', t), accessToken)
    if (me) {
      await page.route('**/api/auth/me', (r) => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ ...user, ...me }) }))
    }
    await page.goto(`${BASE}${path}`, { waitUntil: 'networkidle' })
    await page.waitForTimeout(1200)
    if (before) await before(page)
    await page.waitForTimeout(900)
    await page.screenshot({ path: `${OUT}/${name}.png` })
    if (after) await after(page)
    console.log('ok', name)
  } catch (e) {
    console.log('FAIL', name, e.message.split('\n')[0])
  } finally {
    await page.close()
  }
}

const clickText = (text) => async (page) => { await page.getByRole('button', { name: text }).first().click() }

await shot('01-login', '/login')
await shot('02-dashboard', '/dashboard')
await shot('03-issues', '/issues')
await shot('04-issue-form', '/issues', { before: async (p) => p.click('[data-testid="new-issue-btn"]') })
await shot('05-wbs-grid', `/projects/${PROJECT_ID}/wbs`)
// Grid/Gantt 전환은 radio 버튼이라 글자로 찾아 누른다
await shot('06-wbs-gantt', `/projects/${PROJECT_ID}/wbs`, { before: async (p) => p.click('.el-radio-button:has-text("Gantt")') })
await shot('07-wbs-form', `/projects/${PROJECT_ID}/wbs`, {
  before: async (p) => {
    // 세부 항목(2단계) 행 하나를 선택하고 [편집]을 누른다
    await p.locator('.ag-center-cols-container .ag-row').nth(1).click()
    await p.waitForTimeout(400)
    await clickText('편집')(p)
  }
})
await shot('08-admin-projects', '/admin/projects')
await shot('09-admin-project-form', '/admin/projects', { before: async (p) => p.click('[data-testid="new-project-btn"]') })
await shot('10-admin-assignments', `/admin/projects/${PROJECT_ID}/assignments`)
await shot('11-admin-assignment-form', `/admin/projects/${PROJECT_ID}/assignments`, { before: async (p) => p.click('[data-testid="new-assignment-btn"]') })
await shot('12-admin-users', '/admin/users')
await shot('13-admin-user-form', '/admin/users', { before: async (p) => p.click('[data-testid="new-user-btn"]') })
await shot('14-admin-timesheet', '/admin/timesheet')
await shot('15-admin-timesheet-gantt', '/admin/timesheet', { before: async (p) => p.click('#tab-gantt') })
await shot('16-admin-plan-vs-actual', '/admin/plan-vs-actual')
await shot('17-skin-dark', '/dashboard', { me: { theme: 'DARK' } })
await shot('18-readonly-user', '/admin/projects', { me: { roles: ['ROLE_USER'], name: '일반사용자', username: 'viewer' } })

await browser.close()
