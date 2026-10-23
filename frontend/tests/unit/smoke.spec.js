// 엔지니어 스모크 테스트 (전체 테스트는 QA 담당)
import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { validateFile, MAX_FILE_SIZE } from '@/utils/file'
import { useWbsStore } from '@/stores/wbs'

describe('validateFile', () => {
  it('정확히 2MB는 허용, 1바이트 초과는 TOO_LARGE', () => {
    expect(validateFile({ name: 'a.pdf', size: MAX_FILE_SIZE }).ok).toBe(true)
    expect(validateFile({ name: 'a.pdf', size: MAX_FILE_SIZE + 1 }).reason).toBe('TOO_LARGE')
    expect(validateFile({ name: 'a.exe', size: 10 }).reason).toBe('TYPE')
    expect(validateFile({ name: 'a.txt', size: 0 }).reason).toBe('EMPTY')
  })
})

describe('useWbsStore helpers', () => {
  beforeEach(() => setActivePinia(createPinia()))

  function seed(store) {
    // DFS 순서: 1, 1.1, 1.2, 2
    store.items = [
      { id: 1, parentId: null, sortOrder: 0, level: 1, hasChildren: true, childCount: 2 },
      { id: 11, parentId: 1, sortOrder: 0, level: 2, hasChildren: false, childCount: 0 },
      { id: 12, parentId: 1, sortOrder: 1, level: 2, hasChildren: false, childCount: 0 },
      { id: 2, parentId: null, sortOrder: 1, level: 1, hasChildren: false, childCount: 0 }
    ]
  }

  it('접힌 노드의 하위는 visibleRows에서 제외', () => {
    const store = useWbsStore()
    seed(store)
    store.toggle(1)
    expect(store.visibleRows.map((r) => r.id)).toEqual([1, 2])
  })

  it('indent / outdent / moveUp 대상 계산', () => {
    const store = useWbsStore()
    seed(store)
    expect(store.indentTarget(2)).toEqual({ parentId: 1, index: 2 })
    expect(store.indentTarget(1)).toBeNull()
    expect(store.outdentTarget(12)).toEqual({ parentId: null, index: 1 })
    expect(store.outdentTarget(2)).toBeNull()
    expect(store.moveUpTarget(12)).toEqual({ parentId: 1, index: 0 })
    expect(store.moveDownTarget(12)).toBeNull()
  })
})
