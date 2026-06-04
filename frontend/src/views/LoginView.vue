<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { apiSso } from '@/services/auth'
import { setToken } from '@/services/request'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const loading = ref(false)

const form = reactive({
  tenantCode: 'hd-sales',
  account: 'zhangwei',
  password: '123456',
})

async function onSubmit() {
  if (!form.account || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await auth.login({
      tenantCode: form.tenantCode || undefined,
      account: form.account,
      password: form.password,
    })
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } finally {
    loading.value = false
  }
}

/** 算力平台 SSO：URL 带 ?code=xxx 时自动登录 */
async function trySso() {
  const code = route.query.code as string
  if (!code) return
  loading.value = true
  try {
    const res = await apiSso(code)
    // SSO 返回格式与 LoginResult 一致，直接设置 store
    setToken(res.token)
    auth.token = res.token
    auth.userId = res.userId
    auth.tenantId = res.tenantId
    auth.username = res.username
    auth.realName = res.realName
    auth.permissions = res.permissions
    ElMessage.success('SSO 登录成功')
    router.push('/dashboard')
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || 'SSO 登录失败，请使用账号密码登录')
  } finally {
    loading.value = false
  }
}

onMounted(trySso)
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand-xl">
        <span class="logo-mark">B</span>
        <div>
          <h1>商机管理系统</h1>
          <p>多租户 · 多用户 · 多组织协同</p>
        </div>
      </div>

      <el-form label-position="top" @submit.prevent="onSubmit">
        <el-form-item label="租户编码（平台超管留空）">
          <el-input v-model="form.tenantCode" placeholder="如 hd-sales" clearable />
        </el-form-item>
        <el-form-item label="账号（用户名或手机号）">
          <el-input v-model="form.account" placeholder="账号" clearable />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password
            @keyup.enter="onSubmit" />
        </el-form-item>
        <el-button class="full" type="primary" size="large" :loading="loading" @click="onSubmit">
          登录
        </el-button>
      </el-form>

      <p style="margin-top:16px;color:var(--muted);font-size:12px;">
        演示账号：zhangwei / lina / wangqiang / tadmin / auditor（租户 hd-sales），
        platform_admin（平台，留空租户码）。口令统一 123456。
      </p>
    </div>

    <div class="login-hero">
      <div class="hero-card">
        <div class="eyebrow">BOMS V1.0</div>
        <h2>把每一条商机<br />跑成闭环</h2>
        <p>新增 → 跟进 → 协作 → 成交，多租户隔离，权限到操作。</p>
        <div class="hero-grid">
          <div><b>21</b><span>核心数据表</span></div>
          <div><b>71</b><span>权限码</span></div>
          <div><b>5</b><span>内置角色</span></div>
        </div>
      </div>
    </div>
  </div>
</template>
