import { createRouter, createWebHistory } from 'vue-router'
import { authGuard } from './guards'

export const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
      { path: 'issues', name: 'issues', component: () => import('@/views/issues/IssueListView.vue') },
      {
        path: 'issues/:id(\\d+)',
        name: 'issue-detail',
        component: () => import('@/views/issues/IssueListView.vue')
      },
      { path: 'wbs', name: 'wbs', component: () => import('@/views/wbs/WbsView.vue') },
      {
        path: 'projects/:projectId(\\d+)/wbs',
        name: 'project-wbs',
        component: () => import('@/views/wbs/WbsView.vue')
      },
      {
        path: 'admin',
        component: () => import('@/layouts/AdminLayout.vue'),
        meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
        redirect: '/admin/projects',
        children: [
          {
            path: 'projects',
            name: 'admin-projects',
            component: () => import('@/views/admin/ProjectManageView.vue')
          },
          {
            path: 'projects/:id(\\d+)/assignments',
            name: 'admin-assignments',
            component: () => import('@/views/admin/AssignmentManageView.vue')
          },
          { path: 'users', name: 'admin-users', component: () => import('@/views/admin/UserManageView.vue') },
          {
            path: 'timesheet',
            name: 'admin-timesheet',
            component: () => import('@/views/admin/TimesheetView.vue')
          },
          {
            path: 'plan-vs-actual',
            name: 'admin-plan-vs-actual',
            component: () => import('@/views/admin/PlanVsActualView.vue')
          }
        ]
      }
    ]
  },
  { path: '/forbidden', name: 'forbidden', component: () => import('@/views/ForbiddenView.vue'), meta: { public: true } },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue'), meta: { public: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(authGuard)

export default router
