// QA 체크리스트 3 (FE): 투입/철수일 순서와 프로젝트 기간 검증 → 위반 시 API 미호출
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createPinia } from 'pinia'
import { mountWith, settle, click } from './helpers'
import AssignmentFormModal from '@/components/admin/AssignmentFormModal.vue'
import { createAssignment, updateAssignment } from '@/api/admin'

vi.mock('@/api/admin', () => ({ createAssignment: vi.fn(), updateAssignment: vi.fn() }))

const project = { id: 1, code: 'SI-2026-001', plannedStartDate: '2026-01-01', plannedEndDate: '2026-03-31' }
const users = [{ id: 4, username: 'lee', name: '이영희' }]

function assignment(overrides) {
  return {
    id: 2, user: { id: 4, username: 'lee', name: '이영희' }, roleInProject: 'DEVELOPER', allocationRate: 0.5,
    plannedStart: '2026-02-01', plannedEnd: '2026-03-31', actualStart: '2026-02-01', actualEnd: null, note: null,
    ...overrides
  }
}

function mountForm(a) {
  return mountWith(AssignmentFormModal, {
    props: { modelValue: true, project, users, assignment: a },
    pinia: createPinia()
  })
}

describe('AssignmentFormModal 날짜 검증', () => {
  let wrapper
  beforeEach(() => {
    vi.clearAllMocks()
    document.body.innerHTML = ''
  })
  afterEach(() => wrapper?.unmount())

  async function submit() {
    await settle()
    await click('[data-testid="assignment-form-submit"]')
  }

  it('계획 종료일 < 계획 시작일 → 폼 에러, API 미호출', async () => {
    wrapper = mountForm(assignment({ plannedStart: '2026-03-10', plannedEnd: '2026-03-01' }))
    await submit()
    expect(document.body.textContent).toContain('종료일은 시작일과 같거나 이후여야 합니다.')
    expect(updateAssignment).not.toHaveBeenCalled()
  })

  it('실제 철수일 < 실제 투입일 → 폼 에러, API 미호출', async () => {
    wrapper = mountForm(assignment({ actualStart: '2026-03-10', actualEnd: '2026-03-01' }))
    await submit()
    expect(document.body.textContent).toContain('철수일은 투입일과 같거나 이후여야 합니다.')
    expect(updateAssignment).not.toHaveBeenCalled()
  })

  it('투입일 없이 철수일만 입력 → 폼 에러', async () => {
    wrapper = mountForm(assignment({ actualStart: null, actualEnd: '2026-03-01' }))
    await submit()
    expect(document.body.textContent).toContain('철수일을 입력하려면 투입일이 필요합니다.')
    expect(updateAssignment).not.toHaveBeenCalled()
  })

  it('계획 기간이 프로젝트 계획 기간을 벗어남 → 폼 에러', async () => {
    wrapper = mountForm(assignment({ plannedStart: '2025-12-15', plannedEnd: '2026-04-15' }))
    await submit()
    expect(document.body.textContent).toContain('프로젝트 계획 시작일(2026-01-01) 이후여야 합니다.')
    expect(document.body.textContent).toContain('프로젝트 계획 종료일(2026-03-31) 이전이어야 합니다.')
    expect(updateAssignment).not.toHaveBeenCalled()
  })

  it('같은 날 투입/철수, 경계일 계획 기간은 허용 → update 호출', async () => {
    updateAssignment.mockResolvedValue(assignment({}))
    wrapper = mountForm(assignment({ plannedStart: '2026-01-01', plannedEnd: '2026-03-31', actualStart: '2026-03-01', actualEnd: '2026-03-01' }))
    await submit()
    expect(updateAssignment).toHaveBeenCalledWith(1, 2, expect.objectContaining({
      userId: 4, plannedStart: '2026-01-01', plannedEnd: '2026-03-31', actualStart: '2026-03-01', actualEnd: '2026-03-01'
    }))
    expect(wrapper.emitted('saved')).toHaveLength(1)
  })

  it('신규 등록: 사용자 미선택이면 차단 (기본 계획 기간 = 프로젝트 기간)', async () => {
    wrapper = mountForm(null)
    await submit()
    expect(document.body.textContent).toContain('사용자를 선택하세요.')
    expect(createAssignment).not.toHaveBeenCalled()
  })

  it('서버 400 ASSIGNMENT_OUT_OF_PROJECT_PERIOD fieldErrors를 필드에 표시', async () => {
    updateAssignment.mockRejectedValue({
      response: { status: 400, data: { code: 'ASSIGNMENT_OUT_OF_PROJECT_PERIOD', fieldErrors: [{ field: 'plannedEnd', message: '서버: 기간 밖' }] } }
    })
    wrapper = mountForm(assignment({}))
    await submit()
    expect(document.body.textContent).toContain('서버: 기간 밖')
  })
})
