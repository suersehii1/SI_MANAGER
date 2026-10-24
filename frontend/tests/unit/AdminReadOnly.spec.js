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

  it('사용자 표: USER는 수정 버튼이 없다', async () => {
    wrapper = mountWith(UserTable, { props: { rows: [user], canEdit: false } })
    await waitGrid()
    expect(has('user-edit-3')).toBe(false)
    expect(document.body.textContent).toContain('김철수')
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
