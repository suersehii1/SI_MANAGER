// WBS 화면 컴포넌트: canEdit=false면 편집 버튼 미노출, WbsFormModal 날짜 역전 시 저장 차단 (specs-wbs §W7)
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { mountWith, settle, typeInto, inputOf, click, $ } from './helpers'
import WbsToolbar from '@/components/wbs/WbsToolbar.vue'
import WbsFormModal from '@/components/wbs/WbsFormModal.vue'
import { useWbsStore } from '@/stores/wbs'

vi.mock('@/api/wbs', () => ({
  getWbsTree: vi.fn(() => Promise.resolve({ project: {}, items: [], overallProgress: 0, canEdit: true })),
  createWbsItem: vi.fn(() => Promise.resolve({ id: 99 })),
  updateWbsItem: vi.fn(() => Promise.resolve({ id: 22 })),
  deleteWbsItem: vi.fn(),
  moveWbsItem: vi.fn()
}))
vi.mock('@/api/projects', () => ({ listProjects: vi.fn(), listMembers: vi.fn(() => Promise.resolve([])) }))

const project = { id: 1, code: 'SI-2026-001', plannedStartDate: '2026-01-01', plannedEndDate: '2026-03-31' }
const items = [
  { id: 2, parentId: null, sortOrder: 0, level: 1, hasChildren: true, childCount: 1, rolledUp: true, wbsCode: '1', name: '설계',
    status: 'IN_PROGRESS', weight: 1, progress: 80, plannedStart: '2026-02-01', plannedEnd: '2026-02-28' },
  { id: 22, parentId: 2, sortOrder: 0, level: 2, hasChildren: false, childCount: 0, rolledUp: false, wbsCode: '1.1', name: 'DB 설계',
    status: 'IN_PROGRESS', weight: 1, progress: 80, plannedStart: '2026-02-01', plannedEnd: '2026-02-28',
    actualStart: '2026-02-01', actualEnd: null, assignee: { id: 3 } }
]

function setupStore(canEdit) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useWbsStore()
  Object.assign(store, { projectId: 1, project, items, canEdit, overallProgress: 80 })
  return { pinia, store }
}

describe('WbsToolbar', () => {
  let wrapper
  afterEach(() => wrapper?.unmount())

  it('canEdit=false(비멤버 USER): 편집 버튼 미노출, 조회 전용 안내', () => {
    const { pinia } = setupStore(false)
    wrapper = mountWith(WbsToolbar, { props: { projectId: 1 }, pinia })
    for (const id of ['wbs-add-root', 'wbs-add-child', 'wbs-edit', 'wbs-move-up', 'wbs-move-down', 'wbs-indent', 'wbs-outdent', 'wbs-delete']) {
      expect(wrapper.find(`[data-testid="${id}"]`).exists()).toBe(false)
    }
    expect(wrapper.text()).toContain('조회 전용')
    expect(wrapper.find('[data-testid="wbs-overall-progress"]').exists()).toBe(true)
  })

  it('canEdit=true: 버튼 노출, 선택이 없으면 선택 필요 버튼 비활성', async () => {
    const { pinia, store } = setupStore(true)
    wrapper = mountWith(WbsToolbar, { props: { projectId: 1 }, pinia })
    expect(wrapper.find('[data-testid="wbs-add-root"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.find('[data-testid="wbs-delete"]').attributes('disabled')).toBeDefined()

    // 루트 '설계' 선택: 삭제/하위 추가 가능, 들여쓰기·내어쓰기는 불가(첫 형제·루트)
    store.selectedId = 2
    await settle()
    expect(wrapper.find('[data-testid="wbs-delete"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.find('[data-testid="wbs-add-child"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.find('[data-testid="wbs-indent"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-testid="wbs-outdent"]').attributes('disabled')).toBeDefined()

    // 자식 'DB 설계' 선택: 내어쓰기 가능
    store.selectedId = 22
    await settle()
    expect(wrapper.find('[data-testid="wbs-outdent"]').attributes('disabled')).toBeUndefined()
  })
})

describe('WbsFormModal', () => {
  let wrapper
  beforeEach(() => {
    vi.clearAllMocks()
    document.body.innerHTML = ''
  })
  afterEach(() => wrapper?.unmount())

  async function open(props, canEdit = true) {
    const { pinia, store } = setupStore(canEdit)
    vi.spyOn(store, 'update')
    vi.spyOn(store, 'create')
    wrapper = mountWith(WbsFormModal, { props: { modelValue: false, ...props }, pinia })
    await wrapper.setProps({ modelValue: true })
    await settle()
    return store
  }

  it('edit: 리프 항목 pre-fill, 계획 종료일 < 시작일이면 저장 차단', async () => {
    const leaf = { ...items[1], plannedStart: '2026-02-20', plannedEnd: '2026-02-10' }
    const store = await open({ mode: 'edit', item: leaf })
    expect(inputOf('wbs-form-name').value).toBe('DB 설계')
    expect(document.body.textContent).toContain('1 설계') // 상위 항목 표시

    await click('[data-testid="wbs-form-submit"]')
    expect(document.body.textContent).toContain('종료일은 시작일과 같거나 이후여야 합니다.')
    expect(store.update).not.toHaveBeenCalled()
  })

  it('O-02: 열린 상태(modelValue=true)로 처음 마운트돼도 pre-fill', async () => {
    const { pinia } = setupStore(true)
    wrapper = mountWith(WbsFormModal, { props: { modelValue: true, mode: 'edit', item: items[1] }, pinia })
    await settle()
    expect(inputOf('wbs-form-name').value).toBe('DB 설계')
  })

  it('edit: 실제 종료일만 있고 시작일이 없으면 저장 차단', async () => {
    const leaf = { ...items[1], actualStart: null, actualEnd: '2026-02-10' }
    const store = await open({ mode: 'edit', item: leaf })
    await click('[data-testid="wbs-form-submit"]')
    expect(document.body.textContent).toContain('종료일을 입력하려면 시작일이 필요합니다.')
    expect(store.update).not.toHaveBeenCalled()
  })

  it('edit: 정상 값이면 update(id, 행 전체 payload)', async () => {
    const store = await open({ mode: 'edit', item: items[1] })
    await typeInto('wbs-form-name', 'DB 상세 설계')
    await click('[data-testid="wbs-form-submit"]')
    expect(store.update).toHaveBeenCalledWith(22, expect.objectContaining({
      name: 'DB 상세 설계', assigneeId: 3, status: 'IN_PROGRESS', progress: 80,
      plannedStart: '2026-02-01', plannedEnd: '2026-02-28', actualStart: '2026-02-01', actualEnd: null
    }))
    expect(wrapper.emitted('saved')).toHaveLength(1)
  })

  it('edit: 집계(rolledUp) 항목은 상태/기간/진척 입력 비활성 + 안내', async () => {
    await open({ mode: 'edit', item: items[0] })
    expect(document.body.textContent).toContain('하위 항목에서 자동 집계')
    const disabledPickers = document.body.querySelectorAll('.el-date-editor.is-disabled')
    expect(disabledPickers.length).toBe(4)
  })

  it('create: 이름 필수, 입력 후 부모/삽입 위치와 함께 create', async () => {
    const store = await open({ mode: 'create', parent: items[0], insertAfterId: 22 })
    await click('[data-testid="wbs-form-submit"]')
    expect(document.body.textContent).toContain('작업명을 입력하세요.')
    expect(store.create).not.toHaveBeenCalled()

    await typeInto('wbs-form-name', '인덱스 설계')
    await click('[data-testid="wbs-form-submit"]')
    expect(store.create).toHaveBeenCalledWith(expect.objectContaining({
      name: '인덱스 설계', parentId: 2, insertAfterId: 22, status: 'NOT_STARTED', progress: 0, weight: 1
    }))
  })

  it('프로젝트 계획 기간 밖이면 경고 표시 (저장은 서버가 판단)', async () => {
    const leaf = { ...items[1], plannedStart: '2026-03-20', plannedEnd: '2026-04-10' }
    await open({ mode: 'edit', item: leaf })
    expect(document.body.textContent).toContain('프로젝트 계획 기간(2026-01-01 ~ 2026-03-31)을 벗어났습니다.')
  })
})
