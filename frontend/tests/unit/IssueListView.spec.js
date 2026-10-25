// QA 체크리스트 4 (FE): 신규 이슈 팝업 → 저장 → 메인 Grid 즉시 반영
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { mountWith, settle, typeInto, inputOf, click, $ } from './helpers'
import IssueListView from '@/views/issues/IssueListView.vue'
import IssueTable from '@/components/issues/IssueTable.vue'
import { useIssueStore } from '@/stores/issue'
import { useAuthStore } from '@/stores/auth'
import { listIssues, createIssue, getIssue, updateIssue } from '@/api/issues'

vi.mock('@/api/issues', () => ({
  listIssues: vi.fn(), getIssue: vi.fn(), createIssue: vi.fn(), updateIssue: vi.fn(), deleteIssue: vi.fn()
}))
vi.mock('@/api/projects', () => ({
  listProjects: vi.fn(() => Promise.resolve([{ id: 1, code: 'SI-2026-001', name: '차세대 ERP 구축' }])),
  listMembers: vi.fn(() => Promise.resolve([]))
}))
vi.mock('@/api/users', () => ({ listUsers: vi.fn(() => Promise.resolve([{ id: 3, username: 'kim', name: '김철수' }])) }))
vi.mock('@/api/attachments', () => ({ uploadAttachment: vi.fn(), deleteAttachment: vi.fn(), downloadAttachment: vi.fn() }))

const seedRow = {
  id: 1, projectId: 1, projectCode: 'SI-2026-001', title: '로그인 화면 오류', status: 'OPEN', priority: 'HIGH',
  progress: 0, assignee: null, reporter: { id: 1 }, dueDate: null, attachmentCount: 0, createdAt: '2026-10-23T09:00:00+09:00'
}
const page = (rows) => ({ content: rows, totalElements: rows.length, totalPages: 1, page: 0, size: 20 })

async function mountView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const auth = useAuthStore()
  auth.token = 't'
  auth.user = { id: 1, username: 'admin', roles: ['ROLE_ADMIN'] }
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/issues', name: 'issues', component: IssueListView },
      { path: '/issues/:id', name: 'issue-detail', component: IssueListView }
    ]
  })
  await router.push('/issues')
  const wrapper = mountWith(IssueListView, { pinia, router })
  await settle()
  return wrapper
}

describe('IssueListView', () => {
  let wrapper
  beforeEach(() => {
    vi.clearAllMocks()
    document.body.innerHTML = ''
  })
  afterEach(() => wrapper?.unmount())

  it('신규 이슈 저장 후 목록을 다시 조회해 새 행이 Grid에 나타난다', async () => {
    const created = { ...seedRow, id: 99, title: 'QA 신규 이슈', attachments: [] }
    listIssues.mockResolvedValueOnce(page([seedRow])).mockResolvedValue(page([created, seedRow]))
    createIssue.mockResolvedValue(created)

    wrapper = await mountView()
    expect(listIssues).toHaveBeenCalledTimes(1)
    const store = useIssueStore()
    store.filters.projectId = 1 // 프로젝트 필터가 걸린 상태 → 모달의 기본 프로젝트

    await click('[data-testid="new-issue-btn"]')
    expect($('[data-testid="issue-form-modal"]')).not.toBeNull()
    await typeInto('issue-form-title', '  QA 신규 이슈 ')
    await click('[data-testid="issue-form-submit"]')

    expect(createIssue).toHaveBeenCalledWith({
      projectId: 1, title: 'QA 신규 이슈', description: null, status: 'OPEN', priority: 'MEDIUM',
      progress: 0, assigneeId: null, dueDate: null, version: null // 신규 등록은 버전 없음
    })
    // 저장 직후 Grid 재조회 → 새 행이 맨 위
    expect(listIssues).toHaveBeenCalledTimes(2)
    expect(store.items[0].id).toBe(99)
    expect(wrapper.findComponent(IssueTable).props('rows').map((r) => r.title)).toEqual(['QA 신규 이슈', '로그인 화면 오류'])
  })

  it('다른 페이지를 보고 있어도 신규 등록 후 첫 페이지로 돌아와 새 행이 보인다', async () => {
    const created = { ...seedRow, id: 99, title: 'QA 신규 이슈', attachments: [] }
    listIssues.mockResolvedValue(page([seedRow]))
    createIssue.mockResolvedValue(created)
    wrapper = await mountView()
    const store = useIssueStore()
    store.filters.projectId = 1
    store.page = 2

    listIssues.mockResolvedValue(page([created, seedRow]))
    await click('[data-testid="new-issue-btn"]')
    await typeInto('issue-form-title', 'QA 신규 이슈')
    await click('[data-testid="issue-form-submit"]')

    expect(listIssues).toHaveBeenLastCalledWith(expect.objectContaining({ page: 0 }))
    expect(store.page).toBe(0)
    expect(store.items[0].id).toBe(99)
  })

  it('제목이 비어 있으면 API를 호출하지 않는다', async () => {
    listIssues.mockResolvedValue(page([seedRow]))
    wrapper = await mountView()
    useIssueStore().filters.projectId = 1
    await click('[data-testid="new-issue-btn"]')
    await typeInto('issue-form-title', '   ')
    await click('[data-testid="issue-form-submit"]')
    expect(createIssue).not.toHaveBeenCalled()
    expect(document.body.textContent).toContain('제목을 입력하세요.')
  })

  it('수정 버튼 → 상세 조회 후 수정 팝업 저장 → Grid 재조회', async () => {
    const detail = { ...seedRow, description: '설명', assignee: { id: 3, name: '김철수' }, attachments: [] }
    const updated = { ...detail, title: '로그인 화면 오류(수정)' }
    listIssues.mockResolvedValueOnce(page([seedRow])).mockResolvedValue(page([updated]))
    getIssue.mockResolvedValue(detail)
    updateIssue.mockResolvedValue(updated)
    wrapper = await mountView()

    // 그리드의 수정 버튼 클릭과 같은 이벤트
    wrapper.findComponent(IssueTable).vm.$emit('edit', seedRow)
    await settle()
    expect(getIssue).toHaveBeenCalledWith(1)
    expect(inputOf('issue-form-title').value).toBe('로그인 화면 오류')

    await typeInto('issue-form-title', '로그인 화면 오류(수정)')
    await click('[data-testid="issue-form-submit"]')

    expect(updateIssue).toHaveBeenCalledWith(1, expect.objectContaining({
      title: '로그인 화면 오류(수정)', projectId: 1, status: 'OPEN', priority: 'HIGH', assigneeId: 3, description: '설명'
    }))
    expect(listIssues).toHaveBeenCalledTimes(2)
    expect(useIssueStore().items[0].title).toBe('로그인 화면 오류(수정)')
  })
})
