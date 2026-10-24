// 로그인 화면: 회사 로고(에이젠텀)와 시스템 이름 표시
import { describe, it, expect, afterEach } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia } from 'pinia'
import { mountWith, settle, $ } from './helpers'
import LoginView from '@/views/LoginView.vue'

describe('LoginView 로고', () => {
  let wrapper
  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ''
  })

  it('로그인 카드 맨 위에 에이젠텀 로고, 그 아래 시스템 이름', async () => {
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', component: { template: '<div />' } }] })
    router.push('/')
    await router.isReady()
    wrapper = mountWith(LoginView, { pinia: createPinia(), router })
    await settle()

    const logo = $('[data-testid="login-logo"]')
    expect(logo).not.toBeNull()
    expect(logo.getAttribute('alt')).toContain('Aigentum')
    expect(logo.getAttribute('src')).toContain('aigentum-logo')
    // 로고가 시스템 이름보다 먼저(위에) 나온다
    const title = $('.login__title')
    expect(title.textContent).toBe('전사 Project 관리 시스템')
    expect(logo.compareDocumentPosition(title) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })
})
