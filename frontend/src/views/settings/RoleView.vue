<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { roleApi } from '@/services/system'
import type { RoleRow, PermissionRow } from '@/types'

const roles = ref<RoleRow[]>([])
const allPerms = ref<PermissionRow[]>([])
const loading = ref(false)

const scopeLabels: Record<string, string> = {
  SELF: '本人', DEPT: '本部门', DEPT_AND_SUB: '本部门及下级', TENANT: '全租户', PLATFORM: '平台',
}

async function load() {
  loading.value = true
  try {
    roles.value = await roleApi.list()
    allPerms.value = await roleApi.permissions()
  } finally {
    loading.value = false
  }
}

// 权限按模块分组，组装成 el-tree（模块节点 + 权限叶子）
const permTree = computed(() => {
  const byModule: Record<string, PermissionRow[]> = {}
  for (const p of allPerms.value) {
    const m = p.module || '其他'
    ;(byModule[m] ??= []).push(p)
  }
  return Object.entries(byModule).map(([m, list]) => ({
    id: 'mod:' + m,
    label: m,
    children: list.map((p) => ({ id: p.id, label: `${p.name}　${p.code}` })),
  }))
})

/* ---- 权限配置弹窗 ---- */
const permDialog = reactive({ visible: false, roleId: 0, roleName: '', dataScope: 'SELF' })
const permTreeRef = ref()

async function openPerm(row: RoleRow) {
  permDialog.visible = true
  permDialog.roleId = row.id
  permDialog.roleName = row.name
  permDialog.dataScope = row.dataScope
  const ids = await roleApi.rolePerms(row.id)
  // 等弹窗渲染后回填勾选
  setTimeout(() => permTreeRef.value?.setCheckedKeys(ids, false), 0)
}
async function savePerm() {
  const tree = permTreeRef.value
  const checked: number[] = (tree.getCheckedKeys(false) as (number | string)[])
    .filter((k) => typeof k === 'number') as number[]
  const half: number[] = []  // 模块半选父节点不算权限
  await roleApi.assignPerms(permDialog.roleId, checked.concat(half), permDialog.dataScope)
  ElMessage.success('权限已更新（用户重新登录后生效）')
  permDialog.visible = false
  load()
}

/* ---- 角色增改删 ---- */
const roleDialog = reactive({ visible: false, editId: 0 })
const roleForm = reactive({ name: '', code: '', dataScope: 'SELF', remark: '' })

function openCreate() {
  roleDialog.visible = true
  roleDialog.editId = 0
  Object.assign(roleForm, { name: '', code: '', dataScope: 'SELF', remark: '' })
}
function openEdit(row: RoleRow) {
  roleDialog.visible = true
  roleDialog.editId = row.id
  Object.assign(roleForm, { name: row.name, code: row.code, dataScope: row.dataScope, remark: row.remark ?? '' })
}
async function submitRole() {
  if (!roleForm.name || !roleForm.code) return ElMessage.warning('请填写角色名与角色码')
  if (roleDialog.editId) {
    await roleApi.update(roleDialog.editId, { ...roleForm })
  } else {
    await roleApi.create({ ...roleForm })
  }
  ElMessage.success('已保存')
  roleDialog.visible = false
  load()
}
async function removeRole(row: RoleRow) {
  await ElMessageBox.confirm(`删除角色「${row.name}」？`, '确认', { type: 'warning' })
  await roleApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="breadcrumb">设置 / 角色权限</div>
        <h1>角色权限</h1>
        <p>配置角色的菜单/操作权限码与数据范围（M04，RBAC + 数据范围）。</p>
      </div>
      <div class="head-actions">
        <el-button v-perm="'role:create'" type="primary" @click="openCreate">新增角色</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="roles" v-loading="loading" stripe>
        <el-table-column prop="name" label="角色" width="140" />
        <el-table-column prop="code" label="角色码" width="160" />
        <el-table-column label="数据范围" width="160">
          <template #default="{ row }">
            <el-tag size="small">{{ scopeLabels[row.dataScope] || row.dataScope }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="160" />
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button v-perm="'role:assign_perm'" link type="primary" @click="openPerm(row as RoleRow)">配置权限</el-button>
            <el-button v-perm="'role:update'" link type="primary" @click="openEdit(row as RoleRow)">编辑</el-button>
            <el-button v-perm="'role:delete'" link type="danger" @click="removeRole(row as RoleRow)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 权限配置 -->
    <el-dialog v-model="permDialog.visible" :title="`配置权限 · ${permDialog.roleName}`" width="560px">
      <el-form-item label="数据范围" label-width="80px">
        <el-select v-model="permDialog.dataScope" style="width:240px">
          <el-option v-for="(label, val) in scopeLabels" :key="val" :label="label" :value="val" />
        </el-select>
      </el-form-item>
      <div style="max-height:420px;overflow:auto;border:1px solid var(--line);border-radius:12px;padding:10px;margin-top:10px;">
        <el-tree ref="permTreeRef" :data="permTree" show-checkbox node-key="id"
          :default-expand-all="false" :props="{ label: 'label', children: 'children' }" />
      </div>
      <template #footer>
        <el-button @click="permDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="savePerm">保存权限</el-button>
      </template>
    </el-dialog>

    <!-- 角色增改 -->
    <el-dialog v-model="roleDialog.visible" :title="roleDialog.editId ? '编辑角色' : '新增角色'" width="440px">
      <el-form label-width="80px">
        <el-form-item label="角色名"><el-input v-model="roleForm.name" /></el-form-item>
        <el-form-item label="角色码">
          <el-input v-model="roleForm.code" :disabled="!!roleDialog.editId" placeholder="如 PRESALES" />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="roleForm.dataScope" style="width:100%">
            <el-option v-for="(label, val) in scopeLabels" :key="val" :label="label" :value="val" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="roleForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
