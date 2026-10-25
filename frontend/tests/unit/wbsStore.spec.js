// WBS 스토어: 접기(visibleRows), 이동 헬퍼(위/아래/들여쓰기/내어쓰기) 계산과 API 연동 (specs-wbs §W5)
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useWbsStore } from '@/stores/wbs'
import { getWbsTree, moveWbsItem, createWbsItem, updateWbsItem, deleteWbsItem } from '@/api/wbs'

vi.mock('@/api/wbs', () => ({
  getWbsTree: vi.fn(), createWbsItem: vi.fn(), updateWbsItem: vi.fn(), deleteWbsItem: vi.fn(), moveWbsItem: vi.fn()
}))
vi.mock('@/api/projects', () => ({ listProjects: vi.fn(), listMembers: vi.fn(() => Promise.resolve([])) }))

// W3.1 시드 구조 (DFS 순서): 1(1.1, 1.2) 2(2.1, 2.2(2.2.1)) 3
function seed() {
  return [
    { id: 1, parentId: null, sortOrder: 0, level: 1, hasChildren: true, childCount: 2, wbsCode: '1' },
    { id: 11, parentId: 1, sortOrder: 0, level: 2, hasChildren: false, childCount: 0, wbsCode: '1.1' },
    { id: 12, parentId: 1, sortOrder: 1, level: 2, hasChildren: false, childCount: 0, wbsCode: '1.2' },
    { id: 2, parentId: null, sortOrder: 1, level: 1, hasChildren: true, childCount: 2, wbsCode: '2' },
    { id: 21, parentId: 2, sortOrder: 0, level: 2, hasChildren: false, childCount: 0, wbsCode: '2.1' },
    { id: 22, parentId: 2, sortOrder: 1, level: 2, hasChildren: true, childCount: 1, wbsCode: '2.2' },
    { id: 221, parentId: 22, sortOrder: 0, level: 3, hasChildren: false, childCount: 0, wbsCode: '2.2.1' },
    { id: 3, parentId: null, sortOrder: 2, level: 1, hasChildren: false, childCount: 0, wbsCode: '3' }
  ]
}

const ids = (rows) => rows.map((r) => r.id)

describe('useWbsStore', () => {
  let store
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    store = useWbsStore()
    store.projectId = 1
    store.items = seed()
  })

  describe('visibleRows (접기)', () => {
    it('기본은 전체 표시', () => {
      expect(ids(store.visibleRows)).toEqual([1, 11, 12, 2, 21, 22, 221, 3])
    })

    it('조상이 접히면 손자까지 숨김, 다시 펼치면 복원', () => {
      store.toggle(2)
      expect(ids(store.visibleRows)).toEqual([1, 11, 12, 2, 3])
      store.toggle(2)
      expect(ids(store.visibleRows)).toEqual([1, 11, 12, 2, 21, 22, 221, 3])
    })

    it('하위 노드만 접으면 그 아래만 숨김', () => {
      store.toggle(22)
      expect(ids(store.visibleRows)).toEqual([1, 11, 12, 2, 21, 22, 3])
    })

    it('모두 접기 / 모두 펼치기', () => {
      store.collapseAll()
      expect(ids(store.visibleRows)).toEqual([1, 2, 3])
      store.expandAll()
      expect(store.visibleRows).toHaveLength(8)
    })
  })

  describe('이동 대상 계산', () => {
    it('moveUp / moveDown: 같은 부모 안에서 ±1, 끝이면 null', () => {
      expect(store.moveUpTarget(12)).toEqual({ parentId: 1, index: 0 })
      expect(store.moveUpTarget(11)).toBeNull()
      expect(store.moveDownTarget(11)).toEqual({ parentId: 1, index: 1 })
      expect(store.moveDownTarget(12)).toBeNull()
      expect(store.moveDownTarget(1)).toEqual({ parentId: null, index: 1 })
      expect(store.moveDownTarget(3)).toBeNull()
    })

    it('indent: 바로 위 형제의 마지막 자식으로, 첫 형제면 null', () => {
      expect(store.indentTarget(2)).toEqual({ parentId: 1, index: 2 })
      expect(store.indentTarget(3)).toEqual({ parentId: 2, index: 2 })
      expect(store.indentTarget(22)).toEqual({ parentId: 21, index: 0 })
      expect(store.indentTarget(1)).toBeNull()
      expect(store.indentTarget(221)).toBeNull()
    })

    it('outdent: 부모의 부모 아래, 부모 바로 뒤로. 루트면 null', () => {
      expect(store.outdentTarget(12)).toEqual({ parentId: null, index: 1 })
      expect(store.outdentTarget(221)).toEqual({ parentId: 2, index: 2 })
      expect(store.outdentTarget(3)).toBeNull()
    })

    it('형제 순서는 items 순서가 아니라 sortOrder 기준', () => {
      store.items = [
        { id: 5, parentId: null, sortOrder: 1, level: 1, childCount: 0 },
        { id: 6, parentId: null, sortOrder: 0, level: 1, childCount: 0 }
      ]
      expect(store.moveUpTarget(5)).toEqual({ parentId: null, index: 0 })
      expect(store.moveUpTarget(6)).toBeNull()
    })
  })

  describe('API 연동', () => {
    const tree = (items) => ({ project: { id: 1 }, items, overallProgress: 53.8, canEdit: true, asOf: '2026-03-15' })

    it('indent는 move API 호출 후 응답 트리 적용, 접힌 새 부모는 펼친다', async () => {
      store.toggle(1)
      moveWbsItem.mockResolvedValue(tree(seed()))
      await store.indent(2)
      expect(moveWbsItem).toHaveBeenCalledWith(2, { parentId: 1, index: 2 })
      expect(store.collapsedIds.has(1)).toBe(false)
      expect(store.overallProgress).toBe(53.8)
    })

    it('불가능한 이동(첫 형제 위로)은 API를 호출하지 않는다', async () => {
      await store.moveUp(11)
      await store.indent(1)
      await store.outdent(3)
      expect(moveWbsItem).not.toHaveBeenCalled()
    })

    it('생성/수정/삭제 후 트리를 다시 조회해 부모 집계값을 갱신한다', async () => {
      getWbsTree.mockResolvedValue(tree(seed()))
      createWbsItem.mockResolvedValue({ id: 99 })
      updateWbsItem.mockResolvedValue({ id: 11 })
      deleteWbsItem.mockResolvedValue()
      await store.create({ name: 'x' })
      await store.update(11, { name: 'y' })
      await store.remove(3)
      expect(createWbsItem).toHaveBeenCalledWith(1, { name: 'x' })
      expect(getWbsTree).toHaveBeenCalledTimes(3)
    })

    it('선택 항목이 삭제되면 선택 해제', () => {
      store.selectedId = 3
      expect(store.selectedItem.id).toBe(3)
      store.applyTree(tree(seed().filter((i) => i.id !== 3)))
      expect(store.selectedId).toBeNull()
      expect(store.selectedItem).toBeNull()
    })
  })

  describe('update (덮어쓰기 방지)', () => {
    const tree = (items) => ({ project: { id: 1 }, items, overallProgress: 0, canEdit: true, asOf: '2026-03-15' })

    it('목록에서 불러온 version을 같이 보낸다', async () => {
      store.items = seed().map((r) => ({ ...r, version: r.id === 11 ? 5 : 0 }))
      updateWbsItem.mockResolvedValue({ id: 11 })
      getWbsTree.mockResolvedValue(tree(seed()))
      await store.update(11, { name: '요구사항 정의' })
      expect(updateWbsItem).toHaveBeenCalledWith(11, { name: '요구사항 정의', version: 5 })
    })

    it('다른 사람이 먼저 저장해서 409면 최신 목록을 다시 불러오고 오류를 넘긴다', async () => {
      store.items = seed().map((r) => ({ ...r, version: 1 }))
      updateWbsItem.mockRejectedValue({ response: { status: 409, data: { code: 'CONCURRENT_MODIFICATION' } } })
      getWbsTree.mockResolvedValue(tree(seed()))
      await expect(store.update(11, { name: 'x' })).rejects.toMatchObject({ response: { status: 409 } })
      expect(getWbsTree).toHaveBeenCalledTimes(1) // 최신 내용 다시 조회
    })
  })
})
