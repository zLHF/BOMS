<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { routes, type MenuGroup } from '@/router'
import { useAuthStore } from '@/stores/auth'
import { apiChangePassword, apiLogout } from '@/services/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

interface MenuItem { path: string; title: string; perm: string; group: MenuGroup }

const menus = computed<MenuItem[]>(() => {
  const layout = routes.find((r) => r.path === '/')
  const children = layout?.children ?? []
  return children
    .filter((c) => c.meta?.group && c.meta?.perm && auth.hasPerm(c.meta.perm as string))
    .map((c) => ({
      path: '/' + c.path,
      title: c.meta!.title as string,
      perm: c.meta!.perm as string,
      group: c.meta!.group as MenuGroup,
    }))
})

const groups: MenuGroup[] = ['业务', '设置', '平台']
const groupedMenus = computed(() =>
  groups
    .map((g) => ({ group: g, items: menus.value.filter((m) => m.group === g) }))
    .filter((g) => g.items.length > 0),
)

const activePath = computed(() => route.path)

function go(path: string) { router.push(path) }

async function logout() {
  try { await apiLogout() } catch { /* 无状态登出，忽略 */ }
  auth.logout()
  router.push('/login')
}

/* 改密 */
const pwd = reactive({ visible: false, oldPassword: '', newPassword: '', confirm: '' })
function openPwd() {
  Object.assign(pwd, { visible: true, oldPassword: '', newPassword: '', confirm: '' })
}
async function submitPwd() {
  if (pwd.newPassword.length < 6) return ElMessage.warning('新密码至少 6 位')
  if (pwd.newPassword !== pwd.confirm) return ElMessage.warning('两次输入不一致')
  await apiChangePassword(pwd.oldPassword, pwd.newPassword)
  ElMessage.success('密码已修改，请重新登录')
  pwd.visible = false
  await logout()
}

function onCommand(cmd: string) {
  if (cmd === 'logout') logout()
  if (cmd === 'pwd') openPwd()
}
</script>

<template>
  <div class="app-root">
    <aside class="sidebar">
      <div class="brand" @click="go('/dashboard')">
        <span class="logo-mark">B</span>
        <span>商机管理</span>
      </div>
      <div class="nav-scroll">
        <template v-for="g in groupedMenus" :key="g.group">
          <div class="nav-title">{{ g.group }}</div>
          <button v-for="m in g.items" :key="m.path" class="nav-item"
            :class="{ active: activePath === m.path }" @click="go(m.path)">
            {{ m.title }}
          </button>
        </template>
      </div>
      <div class="sidebar-foot">
        <span>租户 #{{ auth.tenantId }}{{ auth.isPlatform ? '（平台）' : '' }}</span>
        <a @click="logout">退出登录</a>
      </div>
    </aside>

    <div class="main-shell">
      <header class="topbar">
        <div class="tenant-switch">
          <span>{{ auth.isPlatform ? '平台超管' : '租户：' + auth.tenantId }}</span>
        </div>
        <div class="top-actions">
          <el-dropdown @command="onCommand">
            <div class="user-chip" style="cursor:pointer;">
              <div>
                <b>{{ auth.realName || auth.username }}</b>
                <span>{{ auth.permissions.length }} 项权限</span>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="pwd">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="page-body">
        <RouterView />
      </main>
    </div>

    <el-dialog v-model="pwd.visible" title="修改密码" width="400px">
      <el-form label-width="80px">
        <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password /></el-form-item>
        <el-form-item label="确认"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwd.visible = false">取消</el-button>
        <el-button type="primary" @click="submitPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
