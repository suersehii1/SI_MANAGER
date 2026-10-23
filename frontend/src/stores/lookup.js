import { defineStore } from 'pinia'
import { listProjects } from '@/api/projects'
import { listUsers } from '@/api/users'

// 프로젝트/사용자 선택 목록 캐시 (force=true면 다시 조회)
export const useLookupStore = defineStore('lookup', {
  state: () => ({
    projects: [],
    users: [],
    projectsLoaded: false,
    usersLoaded: false
  }),
  actions: {
    async loadProjects(force = false) {
      if (this.projectsLoaded && !force) return this.projects
      this.projects = await listProjects()
      this.projectsLoaded = true
      return this.projects
    },
    async loadUsers(force = false) {
      if (this.usersLoaded && !force) return this.users
      this.users = await listUsers()
      this.usersLoaded = true
      return this.users
    }
  }
})
