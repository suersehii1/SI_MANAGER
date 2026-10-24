// 라우터 가드: 로그인/역할 검사. Admin 콘솔은 일반 USER도 진입 가능(조회 전용)
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import { routes } from '@/router'
import { authGuard } from '@/router/guards'
import { useAuthStore } from '@/stores/auth'
import { fetchMe } from '@/api/auth'
import { mountWith } from './helpers'
import AppSidebar from '@/components/common/AppSidebar.vue'

vi.mock('@/api/auth', () => ({ login: vi.fn(), fetchMe: vi.fn() }))

const USER = { id: 2, username: 'user', roles: ['ROLE_USER'] }
const ADMIN = { id: 1, username: 'admin', roles: ['ROLE_ADMIN', 'ROLE_USER'] }

function newRouter() {
  const router = createRouter({ history: createMemoryHistory(), routes })
  router.beforeEach(authGuard)
  return router
}

function signIn(user) {
  const auth = useAuthStore()
  auth.token = 'token'
  auth.user = user
  return auth
}

describe('authGuard', () => {
  let router
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
    router = newRouter()
  })

  it('USER → /admin/* 도 통과 (Admin 콘솔은 조회 전용으로 열림)', async () => {
    signIn(USER)
    for (const path of ['/admin', '/admin/projects', '/admin/users', '/admin/timesheet',
      '/admin/plan-vs-actual', '/admin/projects/1/assignments']) {
      expect(await authGuard(router.resolve(path))).toBe(true)
    }
  })

  it('meta.roles가 있는 경로는 역할이 없으면 forbidden (가드의 역할 검사 기능)', async () => {
    router.addRoute({ path: '/admin-only', name: 'admin-only', component: { template: '<div/>' },
      meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] } })
    signIn(USER)
    expect(await authGuard(router.resolve('/admin-only'))).toEqual({ name: 'forbidden' })
    signIn(ADMIN)
    expect(await authGuard(router.resolve('/admin-only'))).toBe(true)
  })

  it('ADMIN → /admin/* 통과', async () => {
    signIn(ADMIN)
    expect(await authGuard(router.resolve('/admin/projects'))).toBe(true)
    expect(await authGuard(router.resolve('/admin/plan-vs-actual'))).toBe(true)
  })

  it('USER → 일반 화면(대시보드, 이슈, WBS) 통과', async () => {
    signIn(USER)
    for (const path of ['/dashboard', '/issues', '/issues/3', '/wbs', '/projects/1/wbs']) {
      expect(await authGuard(router.resolve(path))).toBe(true)
    }
  })

  it('미로그인 → login?redirect=원래경로', async () => {
    expect(await authGuard(router.resolve('/admin/projects'))).toEqual({
      name: 'login', query: { redirect: '/admin/projects' }
    })
    expect(await authGuard(router.resolve('/issues?status=OPEN'))).toEqual({
      name: 'login', query: { redirect: '/issues?status=OPEN' }
    })
  })

  it('토큰만 있으면 /auth/me로 사용자 조회 후 통과', async () => {
    useAuthStore().token = 'token'
    fetchMe.mockResolvedValue(USER)
    expect(await authGuard(router.resolve('/admin/users'))).toBe(true)
    expect(fetchMe).toHaveBeenCalledTimes(1)
  })

  it('토큰이 만료돼 /auth/me 실패 → 로그아웃 후 login', async () => {
    const auth = useAuthStore()
    auth.token = 'expired'
    fetchMe.mockRejectedValue(new Error('401'))
    expect(await authGuard(router.resolve('/dashboard'))).toEqual({ name: 'login', query: { redirect: '/dashboard' } })
    expect(auth.token).toBeNull()
  })

  it('로그인 상태에서 /login → dashboard, 공개 경로는 통과', async () => {
    signIn(USER)
    expect(await authGuard(router.resolve('/login'))).toEqual({ name: 'dashboard' })
    expect(await authGuard(router.resolve('/forbidden'))).toBe(true)
  })

  it('실제 네비게이션: USER가 /admin/users 로 이동하면 사용자 화면(조회 전용)', async () => {
    signIn(USER)
    await router.push('/admin/users')
    expect(router.currentRoute.value.name).toBe('admin-users')
  })
})

describe('AppSidebar', () => {
  beforeEach(() => setActivePinia(createPinia()))

  async function mountSidebar(user) {
    const pinia = createPinia()
    setActivePinia(pinia)
    signIn(user)
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:p(.*)*', component: { template: '<div/>' } }] })
    await router.push('/dashboard')
    return mountWith(AppSidebar, { pinia, router })
  }

  it('Admin 메뉴는 모든 사용자에게 보인다 (일반 USER는 조회 전용)', async () => {
    const asUser = await mountSidebar(USER)
    expect(asUser.find('[data-testid="menu-admin"]').exists()).toBe(true)
    expect(asUser.find('[data-testid="menu-wbs"]').exists()).toBe(true)
    asUser.unmount()

    const asAdmin = await mountSidebar(ADMIN)
    expect(asAdmin.find('[data-testid="menu-admin"]').exists()).toBe(true)
    asAdmin.unmount()
  })
})
