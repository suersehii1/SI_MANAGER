// Admin 콘솔 조회 전용(일반 USER): 표의 수정/삭제 버튼 숨김, 탭 줄에 "조회 전용" 안내
import { describe, it, expect, afterEach } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { mountWith, settle } from './helpers'
import { useAuthStore } from '@/stores/auth'
import ProjectTable from '@/components/admin/ProjectTable.vue'
import UserTable from '@/components/admin/UserTable.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'

const project = {
  id: 7, code: 'P-1', name: '테스트', clientName: null, status: 'IN_PROGRESS',
  plannedStartDate: '2026-01-01', plannedEndDate: '2026-12-31', actualStartDate: null, actualEndDate: null, assignedHeadcount: 1
}
const user = { id: 3, username: 'kim', name: '김철수', email: null, jobTitle: '사원', phone: null, roles: ['ROLE_USER'], enabled: true, createdAt: null }

// AG Grid 렌더링 완료까지 기다린다
async function waitGrid() {
  await settle(10)
  await new Promise((r) => setTimeout(r, 200))
  await settle()
}
const has = (testid) => !!document.body.querySelector(`[data-testid="${testid}"]`)
// 화면에 보이는 컬럼 제목 (왼쪽부터)
const headers = () => [...document.body.querySelectorAll('.ag-header-cell-text')].map((e) => e.textContent.trim())

describe('Admin 조회 전용', () => {
  let wrapper
  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ''
  })

  it('프로젝트 표: ADMIN은 수정/삭제 버튼, USER는 할당 보기·WBS만', async () => {
    wrapper = mountWith(ProjectTable, { props: { rows: [project], canEdit: true } })
    await waitGrid()
    expect(has('project-edit-7')).toBe(true)
    expect(has('project-delete-7')).toBe(true)
    wrapper.unmount()
    document.body.innerHTML = ''

    wrapper = mountWith(ProjectTable, { props: { rows: [project], canEdit: false } })
    await waitGrid()
    expect(has('project-edit-7')).toBe(false)
    expect(has('project-delete-7')).toBe(false)
    expect(has('project-wbs-7')).toBe(true)
    expect(document.body.querySelector('[data-testid="project-assignments-7"]').textContent).toBe('할당 보기')
  })

  it('사용자 표: USER는 수정 버튼과 아이디 컬럼이 없다', async () => {
    wrapper = mountWith(UserTable, { props: { rows: [{ ...user, username: null }], canEdit: false } })
    await waitGrid()
    expect(has('user-edit-3')).toBe(false)
    expect(document.body.textContent).toContain('김철수')
    expect(headers()).not.toContain('아이디')
  })

  it('사용자 표: 이름 옆 영어이름, 이메일 옆 전화번호 (ADMIN은 아이디도)', async () => {
    const row = { ...user, englishName: 'Cheolsu Kim', email: 'kim@example.com', phone: '010-1234-5678' }
    wrapper = mountWith(UserTable, { props: { rows: [row], canEdit: true } })
    await waitGrid()
    const h = headers()
    expect(h.slice(0, 5)).toEqual(['아이디', '이름', '영어이름', '이메일', '전화번호'])
    expect(document.body.textContent).toContain('Cheolsu Kim')
    expect(document.body.textContent).toContain('010-1234-5678')
  })

  it('Admin 탭 줄: USER에게만 "조회 전용" 안내', async () => {
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:p(.*)*', name: 'admin-projects', component: { template: '<div/>' } }] })
    await router.push('/admin/projects')

    const asUser = createPinia()
    setActivePinia(asUser)
    useAuthStore().user = { id: 2, username: 'user', roles: ['ROLE_USER'] }
    wrapper = mountWith(AdminLayout, { pinia: asUser, router })
    await settle()
    expect(has('admin-readonly')).toBe(true)
    wrapper.unmount()
    document.body.innerHTML = ''

    const asAdmin = createPinia()
    setActivePinia(asAdmin)
    useAuthStore().user = { id: 1, username: 'admin', roles: ['ROLE_ADMIN', 'ROLE_USER'] }
    wrapper = mountWith(AdminLayout, { pinia: asAdmin, router })
    await settle()
    expect(has('admin-readonly')).toBe(false)
  })
})
