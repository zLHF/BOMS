import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

/**
 * 登录态 + 菜单权限守卫：
 * - 公开页直接放行；
 * - 无 token → 跳登录；
 * - 有 token 但未加载用户信息 → 先 loadMe（刷新后恢复）；
 * - 目标路由声明了 perm 但用户无该权限码 → 拦回工作台。
 */
export function setupGuard(router: Router): void {
  router.beforeEach(async (to) => {
    const auth = useAuthStore()

    if (to.meta.public) {
      return true
    }
    if (!auth.loggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    // 刷新后内存态丢失，用 token 拉一次
    if (auth.permissions.length === 0) {
      try {
        await auth.loadMe()
      } catch {
        auth.logout()
        return { path: '/login' }
      }
    }
    if (to.meta.perm && !auth.hasPerm(to.meta.perm)) {
      ElMessage.warning('无权访问该模块')
      return { path: '/dashboard' }
    }
    return true
  })
}
