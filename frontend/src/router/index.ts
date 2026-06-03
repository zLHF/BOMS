import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

// 菜单分组（对应顶层导航分区）
export type MenuGroup = '业务' | '设置' | '平台'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    /** 进入该路由所需菜单权限码（对齐 04 权限码表 menu:*）。 */
    perm?: string
    /** 菜单分组；缺省不在侧边栏展示。 */
    group?: MenuGroup
    /** 侧边栏图标（Element Plus 图标名）。 */
    icon?: string
    /** 公开页（无需登录）。 */
    public?: boolean
  }
}

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/',
    component: DefaultLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '工作台', perm: 'menu:dashboard', group: '业务', icon: 'DataLine' },
      },
      {
        path: 'opportunity',
        name: 'opportunity',
        component: () => import('@/views/OpportunityView.vue'),
        meta: { title: '商机管理', perm: 'menu:opportunity', group: '业务', icon: 'Promotion' },
      },
      {
        path: 'opportunity/:id',
        name: 'opportunity-detail',
        component: () => import('@/views/OpportunityDetailView.vue'),
        meta: { title: '商机详情', perm: 'opp:view' },
      },
      {
        path: 'customer',
        name: 'customer',
        component: () => import('@/views/CustomerView.vue'),
        meta: { title: '客户管理', perm: 'menu:customer', group: '业务', icon: 'User' },
      },
      {
        path: 'task',
        name: 'task',
        component: () => import('@/views/TaskView.vue'),
        meta: { title: '任务中心', perm: 'menu:task', group: '业务', icon: 'List' },
      },
      {
        path: 'follow',
        name: 'follow',
        component: () => import('@/views/PlaceholderView.vue'),
        meta: { title: '跟进记录', perm: 'menu:follow', group: '业务', icon: 'ChatLineSquare' },
      },
      {
        path: 'settings/org',
        name: 'settings-org',
        component: () => import('@/views/settings/OrgUserView.vue'),
        meta: { title: '组织用户', perm: 'menu:settings:org', group: '设置', icon: 'OfficeBuilding' },
      },
      {
        path: 'settings/role',
        name: 'settings-role',
        component: () => import('@/views/settings/RoleView.vue'),
        meta: { title: '角色权限', perm: 'menu:settings:role', group: '设置', icon: 'Lock' },
      },
      {
        path: 'settings/stage',
        name: 'settings-stage',
        component: () => import('@/views/settings/StageConfigView.vue'),
        meta: { title: '阶段/编号配置', perm: 'menu:settings:stage', group: '设置', icon: 'SetUp' },
      },
      {
        path: 'settings/audit',
        name: 'settings-audit',
        component: () => import('@/views/settings/AuditView.vue'),
        meta: { title: '操作日志', perm: 'menu:settings:audit', group: '设置', icon: 'Document' },
      },
      {
        path: 'platform/tenant',
        name: 'platform-tenant',
        component: () => import('@/views/platform/TenantView.vue'),
        meta: { title: '租户管理', perm: 'menu:platform:tenant', group: '平台', icon: 'Coordinate' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
