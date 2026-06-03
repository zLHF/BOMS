<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { tenantApi } from '@/services/system'
import type { TenantRow, SysPackageRow } from '@/types'

const tenants = ref<TenantRow[]>([])
const packages = ref<SysPackageRow[]>([])
const loading = ref(false)

const statusMeta: Record<string, { label: string; type: string }> = {
  TRIAL: { label: '试用', type: 'warning' },
  NORMAL: { label: '正常', type: 'success' },
  EXPIRING: { label: '到期提醒', type: 'warning' },
  FROZEN: { label: '冻结', type: 'danger' },
  DISABLED: { label: '停用', type: 'info' },
}

async function load() {
  loading.value = true
  try {
    tenants.value = await tenantApi.list()
    packages.value = await tenantApi.packages()
  } finally {
    loading.value = false
  }
}

const pkgName = (id?: number) => packages.value.find((p) => p.id === id)?.name ?? '-'

/* ---- 新建租户 ---- */
const dialog = reactive({ visible: false })
const form = reactive({ name: '', code: '', domain: '', packageId: undefined as number | undefined, expireAt: '', adminUsername: '', adminMobile: '', adminPassword: '123456' })

function openCreate() {
  dialog.visible = true
  Object.assign(form, { name: '', code: '', domain: '', packageId: packages.value[0]?.id, expireAt: '', adminUsername: 'admin', adminMobile: '', adminPassword: '123456' })
}
async function submit() {
  if (!form.name || !form.code || !form.adminUsername) return ElMessage.warning('请填写租户名、编码、管理员账号')
  const r = await tenantApi.create({ ...form })
  ElMessage.success(`租户已创建，管理员 ${r.adminUsername} / ${r.adminPassword}`)
  dialog.visible = false
  load()
}

async function changeStatus(row: TenantRow, status: string) {
  await ElMessageBox.confirm(`将「${row.name}」状态改为「${statusMeta[status].label}」？`, '确认')
  await tenantApi.status(row.id, status)
  ElMessage.success('状态已更新')
  load()
}
async function changePackage(row: TenantRow, packageId: number) {
  await tenantApi.changePackage(row.id, packageId)
  ElMessage.success('套餐已更新')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="breadcrumb">平台 / 租户管理</div>
        <h1>租户管理</h1>
        <p>平台超管开通/冻结租户、配置套餐（M02）。创建时自动引导默认角色矩阵与管理员账号。</p>
      </div>
      <div class="head-actions">
        <el-button v-perm="'tenant:create'" type="primary" @click="openCreate">开通租户</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="tenants" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="租户名称" min-width="140" />
        <el-table-column prop="code" label="编码" width="130" />
        <el-table-column prop="domain" label="域名" min-width="150" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="(statusMeta[row.status]?.type as any) || 'info'" size="small">
              {{ statusMeta[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="套餐" width="150">
          <template #default="{ row }">
            <el-select :model-value="row.packageId" size="small" style="width:120px"
              v-perm="'tenant:package'" @change="(v:number) => changePackage(row as TenantRow, v)">
              <el-option v-for="p in packages" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-dropdown v-perm="'tenant:status'" @command="(s:string) => changeStatus(row as TenantRow, s)">
              <el-button link type="primary">变更状态<el-icon><arrow-down /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="(m, s) in statusMeta" :key="s" :command="s">{{ m.label }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog.visible" title="开通租户" width="500px">
      <el-form label-width="92px">
        <el-form-item label="租户名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="租户编码"><el-input v-model="form.code" placeholder="登录识别，如 acme" /></el-form-item>
        <el-form-item label="域名"><el-input v-model="form.domain" /></el-form-item>
        <el-form-item label="套餐">
          <el-select v-model="form.packageId" style="width:100%">
            <el-option v-for="p in packages" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="到期日">
          <el-date-picker v-model="form.expireAt" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-divider>初始管理员</el-divider>
        <el-form-item label="管理员账号"><el-input v-model="form.adminUsername" /></el-form-item>
        <el-form-item label="管理员手机"><el-input v-model="form.adminMobile" /></el-form-item>
        <el-form-item label="初始密码"><el-input v-model="form.adminPassword" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submit">开通</el-button>
      </template>
    </el-dialog>
  </div>
</template>
