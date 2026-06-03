import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { apiLogin, apiMe, type LoginParams } from '@/services/auth'
import { setToken, clearToken, getToken } from '@/services/request'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const userId = ref<number | null>(null)
  const tenantId = ref<number | null>(null)
  const username = ref('')
  const realName = ref('')
  const isPlatform = ref(false)
  const permissions = ref<string[]>([])

  const loggedIn = computed(() => !!token.value)
  const permSet = computed(() => new Set(permissions.value))

  /** 操作权限码判定（供 v-perm 与组件使用）。 */
  function hasPerm(code: string): boolean {
    return permSet.value.has(code)
  }

  async function login(params: LoginParams): Promise<void> {
    const res = await apiLogin(params)
    token.value = res.token
    setToken(res.token)
    userId.value = res.userId
    tenantId.value = res.tenantId
    username.value = res.username
    realName.value = res.realName
    permissions.value = res.permissions
  }

  /** 刷新后用 token 拉取当前用户（守卫中调用）。 */
  async function loadMe(): Promise<void> {
    const me = await apiMe()
    userId.value = me.userId
    tenantId.value = me.tenantId
    username.value = me.username
    realName.value = me.realName
    isPlatform.value = me.isPlatform
    permissions.value = me.permissions
  }

  function logout(): void {
    token.value = null
    userId.value = null
    permissions.value = []
    clearToken()
  }

  return {
    token, userId, tenantId, username, realName, isPlatform, permissions,
    loggedIn, hasPerm, login, loadMe, logout,
  }
})
