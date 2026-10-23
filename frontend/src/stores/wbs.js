import { defineStore } from 'pinia'
import * as wbsApi from '@/api/wbs'
import { listMembers } from '@/api/projects'

/**
 * WBS 화면 상태 (specs-wbs §W5).
 * 서버는 트리를 DFS 전위 순서의 평탄한 items로 준다(level, parentId, sortOrder, childCount 포함).
 * AG Grid Community에는 treeData가 없으므로 접기 상태(collapsedIds)로 보이는 행만 골라 rowData로 쓴다.
 */
export const useWbsStore = defineStore('wbs', {
  state: () => ({
    projectId: null,
    project: null,
    items: [],
    overallProgress: 0,
    canEdit: false,
    asOf: null,
    members: [],
    collapsedIds: new Set(),
    selectedId: null,
    loading: false
  }),
  getters: {
    // 조상 중 하나라도 접혀 있으면 숨긴다. items가 DFS 순서이므로 부모가 항상 먼저 나온다.
    visibleRows(state) {
      const byId = new Map()
      for (const item of state.items) byId.set(item.id, item)
      const rows = []
      for (const item of state.items) {
        let hidden = false
        let parentId = item.parentId
        while (parentId != null) {
          if (state.collapsedIds.has(parentId)) {
            hidden = true
            break
          }
          parentId = byId.get(parentId)?.parentId ?? null
        }
        if (!hidden) rows.push(item)
      }
      return rows
    },
    selectedItem(state) {
      return state.items.find((i) => i.id === state.selectedId) || null
    }
  },
  actions: {
    async fetch(projectId, asOf) {
      this.projectId = projectId
      this.loading = true
      try {
        const res = await wbsApi.getWbsTree(projectId, asOf ? { asOf } : undefined)
        this.applyTree(res)
        return res
      } finally {
        this.loading = false
      }
    },
    applyTree(res) {
      this.project = res.project
      this.items = res.items
      this.overallProgress = Number(res.overallProgress)
      this.canEdit = res.canEdit
      this.asOf = res.asOf
      // 선택된 항목이 삭제되었으면 선택 해제
      if (this.selectedId != null && !res.items.some((i) => i.id === this.selectedId)) {
        this.selectedId = null
      }
    },
    async fetchMembers(projectId) {
      this.members = await listMembers(projectId)
      return this.members
    },
    reset() {
      this.projectId = null
      this.project = null
      this.items = []
      this.overallProgress = 0
      this.canEdit = false
      this.selectedId = null
      this.collapsedIds = new Set()
    },
    // 생성/수정/삭제 후에는 부모 집계값 갱신을 위해 트리를 다시 조회한다
    async create(payload) {
      const created = await wbsApi.createWbsItem(this.projectId, payload)
      await this.fetch(this.projectId)
      return created
    },
    async update(id, payload) {
      const updated = await wbsApi.updateWbsItem(id, payload)
      await this.fetch(this.projectId)
      return updated
    },
    async remove(id) {
      await wbsApi.deleteWbsItem(id)
      await this.fetch(this.projectId)
    },
    async move(id, { parentId, index }) {
      const res = await wbsApi.moveWbsItem(id, { parentId, index })
      this.applyTree(res)
      return res
    },
    toggle(id) {
      const next = new Set(this.collapsedIds)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      this.collapsedIds = next
    },
    expandAll() {
      this.collapsedIds = new Set()
    },
    collapseAll() {
      this.collapsedIds = new Set(this.items.filter((i) => i.hasChildren).map((i) => i.id))
    },

    // ----- 이동 헬퍼: 서버 move API에 보낼 {parentId, index}를 계산 (형제 = 같은 parentId, sortOrder 순) -----
    siblingsOf(item) {
      return this.items
        .filter((i) => (i.parentId ?? null) === (item.parentId ?? null))
        .sort((a, b) => a.sortOrder - b.sortOrder)
    },
    /** 위로: 형제 내 index-1. 첫 번째면 null(무시). */
    moveUpTarget(id) {
      const item = this.items.find((i) => i.id === id)
      if (!item) return null
      const idx = this.siblingsOf(item).findIndex((s) => s.id === id)
      if (idx <= 0) return null
      return { parentId: item.parentId ?? null, index: idx - 1 }
    },
    /** 아래로: 형제 내 index+1. 마지막이면 null. */
    moveDownTarget(id) {
      const item = this.items.find((i) => i.id === id)
      if (!item) return null
      const siblings = this.siblingsOf(item)
      const idx = siblings.findIndex((s) => s.id === id)
      if (idx < 0 || idx >= siblings.length - 1) return null
      return { parentId: item.parentId ?? null, index: idx + 1 }
    },
    /** 들여쓰기: 바로 위 형제를 새 부모로, 그 형제의 마지막 자식 위치로. 첫 번째 형제면 null. */
    indentTarget(id) {
      const item = this.items.find((i) => i.id === id)
      if (!item) return null
      const siblings = this.siblingsOf(item)
      const idx = siblings.findIndex((s) => s.id === id)
      if (idx <= 0) return null
      const newParent = siblings[idx - 1]
      return { parentId: newParent.id, index: newParent.childCount }
    },
    /** 내어쓰기: 현재 부모의 부모 아래, 현재 부모 바로 뒤 위치로. 루트면 null. */
    outdentTarget(id) {
      const item = this.items.find((i) => i.id === id)
      if (!item || item.parentId == null) return null
      const parent = this.items.find((i) => i.id === item.parentId)
      if (!parent) return null
      return { parentId: parent.parentId ?? null, index: parent.sortOrder + 1 }
    },
    async moveUp(id) {
      const t = this.moveUpTarget(id)
      if (t) await this.move(id, t)
    },
    async moveDown(id) {
      const t = this.moveDownTarget(id)
      if (t) await this.move(id, t)
    },
    async indent(id) {
      const t = this.indentTarget(id)
      if (t) {
        // 새 부모가 접혀 있으면 펼쳐서 이동 결과가 보이게 한다
        if (this.collapsedIds.has(t.parentId)) this.toggle(t.parentId)
        await this.move(id, t)
      }
    },
    async outdent(id) {
      const t = this.outdentTarget(id)
      if (t) await this.move(id, t)
    }
  }
})
