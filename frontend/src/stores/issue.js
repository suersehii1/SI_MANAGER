import { defineStore } from 'pinia'
import * as issueApi from '@/api/issues'

/**
 * 이슈 Grid 상태. 서버 사이드 페이징/정렬:
 * - page(0-based), size, sort('field,dir') 를 그대로 GET /api/issues 쿼리로 보낸다.
 * - AG Grid는 현재 페이지 행만 rowData로 표시하고, 정렬 변경 이벤트에서 sort를 바꿔 fetchList()를 다시 호출한다.
 */
export const useIssueStore = defineStore('issue', {
  state: () => ({
    items: [],
    total: 0,
    filters: { projectId: null, status: null, priority: null, assigneeId: null, keyword: '' },
    page: 0,
    size: 20,
    sort: 'createdAt,desc',
    loading: false
  }),
  actions: {
    buildParams() {
      const params = { page: this.page, size: this.size, sort: this.sort }
      for (const [key, value] of Object.entries(this.filters)) {
        if (value !== null && value !== undefined && value !== '') {
          params[key] = value
        }
      }
      return params
    },
    async fetchList() {
      this.loading = true
      try {
        const res = await issueApi.listIssues(this.buildParams())
        this.items = res.content
        this.total = res.totalElements
        return res
      } finally {
        this.loading = false
      }
    },
    fetchOne(id) {
      return issueApi.getIssue(id)
    },
    create(payload) {
      return issueApi.createIssue(payload)
    },
    update(id, payload) {
      return issueApi.updateIssue(id, payload)
    },
    async remove(id) {
      await issueApi.deleteIssue(id)
    },
    // 필터 변경 시 첫 페이지부터 다시 조회
    setFilters(f) {
      this.filters = { ...this.filters, ...f }
      this.page = 0
      return this.fetchList()
    },
    setSort(sort) {
      this.sort = sort || 'createdAt,desc'
      this.page = 0
      return this.fetchList()
    },
    setPage(page, size) {
      if (size && size !== this.size) {
        this.size = size
        this.page = 0
      } else {
        this.page = page
      }
      return this.fetchList()
    }
  }
})
