// 사용자별 화면 스킨(낮/밤): 로그인 시 저장된 스킨 적용, 변경 시 서버 저장, 로그아웃 시 낮으로, 좌측 하단 선택 UI
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import { mountWith, settle } from './helpers'
import { useAuthStore } from '@/stores/auth'
import { applyTheme } from '@/utils/theme'
import { login, updateMyTheme } from '@/api/auth'
import AppSidebar from '@/components/common/AppSidebar.vue'

vi.mock('@/api/auth', () => ({ login: vi.fn(), fetchMe: vi.fn(), updateMyTheme: vi.fn() }))

const isDark = () => document.documentElement.classList.contains('dark')

describe('스킨(낮/밤)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
    applyTheme('LIGHT')
  })
  afterEach(() => {
    document.body.innerHTML = ''
    applyTheme('LIGHT')
  })

  it('applyTheme: DARK면 <html class="dark">, 그 외 값은 낮', () => {
    applyTheme('DARK')
    expect(isDark()).toBe(true)
    applyTheme('LIGHT')
    expect(isDark()).toBe(false)
    applyTheme(null)
    expect(isDark()).toBe(false)
  })

  it('로그인하면 사용자에 저장된 스킨(DARK)이 바로 적용되고, 로그아웃하면 낮으로 돌아간다', async () => {
    login.mockResolvedValue({ accessToken: 't', user: { id: 1, username: 'admin', roles: [], theme: 'DARK' } })
    const auth = useAuthStore()
    await auth.login('admin', 'admin1234')
    expect(auth.theme).toBe('DARK')
    expect(isDark()).toBe(true)
    auth.logout()
    expect(auth.theme).toBe('LIGHT')
    expect(isDark()).toBe(false)
  })

  it('setTheme: 화면에 적용하고 서버에 저장한다', async () => {
    updateMyTheme.mockResolvedValue({ id: 1, username: 'admin', roles: [], theme: 'DARK' })
    const auth = useAuthStore()
    auth.user = { id: 1, username: 'admin', roles: [], theme: 'LIGHT' }
    await auth.setTheme('DARK')
    expect(updateMyTheme).toHaveBeenCalledWith('DARK')
    expect(auth.theme).toBe('DARK')
    expect(isDark()).toBe(true)
  })

  it('setTheme: 서버 저장에 실패하면 이전 스킨으로 되돌린다', async () => {
    updateMyTheme.mockRejectedValue(new Error('fail'))
    const auth = useAuthStore()
    auth.user = { id: 1, username: 'admin', roles: [], theme: 'LIGHT' }
    await expect(auth.setTheme('DARK')).rejects.toThrow()
    expect(auth.theme).toBe('LIGHT')
    expect(isDark()).toBe(false)
  })

  it('좌측 하단 스킨 선택: 낮/밤 두 가지, 밤을 고르면 저장 API 호출', async () => {
    updateMyTheme.mockResolvedValue({ id: 1, username: 'admin', roles: [], theme: 'DARK' })
    const pinia = createPinia()
    setActivePinia(pinia)
    useAuthStore().user = { id: 1, username: 'admin', roles: [], theme: 'LIGHT' }
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', component: { template: '<div />' } }] })
    router.push('/')
    await router.isReady()
    const wrapper = mountWith(AppSidebar, { pinia, router })
    await settle()

    document.body.querySelector('[data-testid="theme-select"]').querySelector('.el-select__wrapper, input').click()
    await settle()
    const options = [...document.body.querySelectorAll('.el-select-dropdown__item')]
    expect(options.map((o) => o.textContent.trim())).toEqual(['낮', '밤'])

    options.find((o) => o.textContent.trim() === '밤').click()
    await settle()
    expect(updateMyTheme).toHaveBeenCalledWith('DARK')
    expect(isDark()).toBe(true)
    wrapper.unmount()
  })
})
