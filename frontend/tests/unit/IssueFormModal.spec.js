// QA 체크리스트 4 (FE): 수정 팝업 pre-fill + 변경 저장, 생성 팝업 프로젝트 선택/첨부 대기열
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { mountWith, settle, typeInto, inputOf, click, $, fakeFile } from './helpers'
import IssueFormModal from '@/components/issues/IssueFormModal.vue'
import { createIssue, updateIssue } from '@/api/issues'
import { uploadAttachment } from '@/api/attachments'

vi.mock('@/api/issues', () => ({
  listIssues: vi.fn(), getIssue: vi.fn(), createIssue: vi.fn(), updateIssue: vi.fn(), deleteIssue: vi.fn()
}))
vi.mock('@/api/projects', () => ({
  listProjects: vi.fn(() => Promise.resolve([
    { id: 1, code: 'SI-2026-001', name: '차세대 ERP 구축' },
    { id: 2, code: 'SI-2026-002', name: '물류 MES 고도화' }
  ])),
  listMembers: vi.fn()
}))
vi.mock('@/api/users', () => ({ listUsers: vi.fn(() => Promise.resolve([{ id: 3, username: 'kim', name: '김철수' }])) }))
vi.mock('@/api/attachments', () => ({ uploadAttachment: vi.fn(), deleteAttachment: vi.fn(), downloadAttachment: vi.fn() }))

const issue = {
  id: 10, projectId: 2, title: '주문 API 성능 개선', description: '응답 지연', status: 'IN_PROGRESS',
  priority: 'MEDIUM', progress: 40, assignee: { id: 3, username: 'kim', name: '김철수' }, dueDate: '2026-03-20',
  reporter: { id: 1 }, attachments: [{ id: 7, originalFilename: 'log.txt', sizeBytes: 100, uploadedBy: { id: 1 }, createdAt: '2026-10-23T09:00:00+09:00' }],
  version: 3 // 낙관적 잠금 버전: 수정 저장 시 그대로 보내야 한다
}

function mountModal(props) {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mountWith(IssueFormModal, { props: { modelValue: true, ...props }, pinia })
}

describe('IssueFormModal', () => {
  let wrapper
  beforeEach(() => {
    vi.clearAllMocks()
    document.body.innerHTML = ''
  })
  afterEach(() => wrapper?.unmount())

  it('edit 모드: 기존 값으로 pre-fill', async () => {
    wrapper = mountModal({ mode: 'edit', issue })
    await settle()
    expect(inputOf('issue-form-title').value).toBe('주문 API 성능 개선')
    expect(inputOf('issue-form-progress').value).toBe('40')
    expect(document.body.textContent).toContain('SI-2026-002 물류 MES 고도화') // 선택된 프로젝트 라벨
    expect(document.body.textContent).toContain('log.txt') // 기존 첨부 목록
    expect(document.body.textContent).toContain('이슈 수정')
  })

  it('edit 모드: 제목 변경 후 저장 → update(id, 전체 payload + 불러온 version) 호출, saved emit, 모달 닫힘', async () => {
    updateIssue.mockResolvedValue({ ...issue, title: '변경됨' })
    wrapper = mountModal({ mode: 'edit', issue })
    await settle()
    await typeInto('issue-form-title', '변경됨')
    await click('[data-testid="issue-form-submit"]')

    expect(updateIssue).toHaveBeenCalledWith(10, {
      projectId: 2, title: '변경됨', description: '응답 지연', status: 'IN_PROGRESS', priority: 'MEDIUM',
      progress: 40, assigneeId: 3, dueDate: '2026-03-20', version: 3
    })
    expect(createIssue).not.toHaveBeenCalled()
    expect(wrapper.emitted('saved')[0][0].title).toBe('변경됨')
    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
  })

  it('서버 400 fieldErrors는 해당 폼 항목 아래에 표시되고 모달은 열린 채 유지', async () => {
    updateIssue.mockRejectedValue({
      response: { status: 400, data: { fieldErrors: [{ field: 'progress', message: '100 이하여야 합니다' }] } }
    })
    wrapper = mountModal({ mode: 'edit', issue })
    await settle()
    await click('[data-testid="issue-form-submit"]')
    expect(document.body.textContent).toContain('100 이하여야 합니다')
    expect(wrapper.emitted('saved')).toBeUndefined()
  })

  it('create 모드: 프로젝트 미선택이면 저장 차단', async () => {
    wrapper = mountModal({ mode: 'create' })
    await settle()
    await typeInto('issue-form-title', '새 이슈')
    await click('[data-testid="issue-form-submit"]')
    expect(createIssue).not.toHaveBeenCalled()
    expect(document.body.textContent).toContain('프로젝트를 선택하세요.')
  })

  it('create 모드: 기본 프로젝트 + 대기열 첨부 → 생성 후 순차 업로드', async () => {
    createIssue.mockResolvedValue({ ...issue, id: 55, attachments: [] })
    uploadAttachment.mockResolvedValue({ id: 1 })
    wrapper = mountModal({ mode: 'create', defaultProjectId: 1 })
    await settle()
    await typeInto('issue-form-title', '첨부 있는 이슈')

    // 첨부 선택(대기열): 2MB 이하만 대기열에 들어간다
    const input = $('[data-testid="attachment-input"]')
    const ok = fakeFile('spec.pdf', 2097152)
    Object.defineProperty(input, 'files', { value: [ok], configurable: true })
    input.dispatchEvent(new Event('change'))
    await settle()
    expect(document.body.textContent).toContain('spec.pdf')

    await click('[data-testid="issue-form-submit"]')
    expect(createIssue).toHaveBeenCalledWith(expect.objectContaining({ projectId: 1, title: '첨부 있는 이슈' }))
    expect(uploadAttachment).toHaveBeenCalledWith(55, ok)
    expect(wrapper.emitted('saved')).toHaveLength(1)
  })
})
