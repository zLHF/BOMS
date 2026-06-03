import type { Directive, DirectiveBinding } from 'vue'
import { useAuthStore } from '@/stores/auth'

/**
 * v-perm：按操作权限码显隐元素。
 *   <el-button v-perm="'opp:create'">新增商机</el-button>
 * 无权限时从 DOM 移除（而非仅隐藏），避免被绕过。
 */
export const perm: Directive<HTMLElement, string | string[]> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const auth = useAuthStore()
    const need = binding.value
    const codes = Array.isArray(need) ? need : [need]
    const ok = codes.some((c) => auth.hasPerm(c))
    if (!ok) {
      el.parentNode?.removeChild(el)
    }
  },
}
