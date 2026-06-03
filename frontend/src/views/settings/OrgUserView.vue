<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deptApi, userApi, roleApi } from '@/services/system'
import type { DeptNode, UserRow, RoleRow } from '@/types'

const tree = ref<DeptNode[]>([])
const users = ref<UserRow[]>([])
const total = ref(0)
const roles = ref<RoleRow[]>([])
const loading = ref(false)

const query = reactive({ keyword: '', deptId: undefined as number | undefined, page: 1, size: 20 })

const deptProps = { label: 'name', children: 'children' }
const deptNameMap = ref<Record<number, string>>({})

function flattenDept(nodes: DeptNode[]) {
  for (const n of nodes) {
    deptNameMap.value[n.id] = n.name
    if (n.children?.length) flattenDept(n.children)
  }
}

async function loadTree() {
  tree.value = await deptApi.tree()
  flattenDept(tree.value)
}
async function loadUsers() {
  loading.value = true
  try {
    const p = await userApi.list({ ...query })
    users.value = p.records
    total.value = p.total
  } finally {
    loading.value = false
  }
}
async function loadRoles() {
  roles.value = await roleApi.list()
}

function onDeptClick(node: DeptNode) {
  query.deptId = node.id
  query.page = 1
  loadUsers()
}
function clearDept() {
  query.deptId = undefined
  query.page = 1
  loadUsers()
}

/* ---- 用户表单 ---- */
const userDialog = reactive({ visible: false, editId: 0 })
const userForm = reactive({ username: '', realName: '', mobile: '', email: '', deptId: undefined as number | undefined, roleIds: [] as number[] })

function openCreate() {
  userDialog.visible = true
  userDialog.editId = 0
  Object.assign(userForm, { username: '', realName: '', mobile: '', email: '', deptId: query.deptId, roleIds: [] })
}
async function openEdit(row: UserRow) {
  userDialog.visible = true
  userDialog.editId = row.id
  Object.assign(userForm, { username: row.username, realName: row.realName, mobile: row.mobile ?? '', email: row.email ?? '', deptId: row.deptId, roleIds: [] })
  userForm.roleIds = await userApi.roles(row.id)
}
async function submitUser() {
  if (!userForm.realName) return ElMessage.warning('请填写姓名')
  if (userDialog.editId) {
    await userApi.update(userDialog.editId, { realName: userForm.realName, mobile: userForm.mobile, email: userForm.email, deptId: userForm.deptId })
    await userApi.assignRoles(userDialog.editId, userForm.roleIds)
    ElMessage.success('已更新')
  } else {
    if (!userForm.username) return ElMessage.warning('请填写登录名')
    await userApi.create({ ...userForm })
    ElMessage.success('已创建，初始密码 123456')
  }
  userDialog.visible = false
  loadUsers()
}

async function resetPwd(row: UserRow) {
  await ElMessageBox.confirm(`重置「${row.realName}」的密码为 123456？`, '确认')
  const r = await userApi.resetPwd(row.id)
  ElMessage.success(`已重置为 ${r.password}`)
}
async function disableUser(row: UserRow) {
  await ElMessageBox.confirm(`停用「${row.realName}」？`, '确认', { type: 'warning' })
  await userApi.disable(row.id)
  ElMessage.success('已停用')
  loadUsers()
}

onMounted(() => { loadTree(); loadUsers(); loadRoles() })
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="breadcrumb">设置 / 组织用户</div>
        <h1>组织用户</h1>
        <p>部门树 + 用户管理（M03）。用户列表按当前角色数据范围过滤。</p>
      </div>
      <div class="head-actions">
        <el-button v-perm="'org:user:create'" type="primary" @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <div class="detail-layout" style="grid-template-columns:280px 1fr;">
      <div class="side-panel">
        <h4 style="margin:0 0 12px;display:flex;justify-content:space-between;align-items:center;">
          部门 <el-link type="primary" @click="clearDept">全部</el-link>
        </h4>
        <el-tree :data="tree" :props="deptProps" node-key="id" default-expand-all
          highlight-current @node-click="onDeptClick" />
      </div>

      <div class="detail-main">
        <div class="compact">
          <el-input v-model="query.keyword" placeholder="搜索用户名/姓名/手机" clearable @keyup.enter="loadUsers" />
          <el-button @click="() => { query.page = 1; loadUsers() }">查询</el-button>
          <el-tag v-if="query.deptId" closable @close="clearDept">
            部门：{{ deptNameMap[query.deptId] }}
          </el-tag>
        </div>

        <el-table :data="users" v-loading="loading" stripe>
          <el-table-column prop="username" label="登录名" width="120" />
          <el-table-column prop="realName" label="姓名" width="100" />
          <el-table-column label="部门" width="120">
            <template #default="{ row }">{{ row.deptId ? deptNameMap[row.deptId] : '-' }}</template>
          </el-table-column>
          <el-table-column prop="mobile" label="手机" width="130" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
                {{ row.status === 'ENABLED' ? '启用' : (row.status === 'DISABLED' ? '停用' : row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="220">
            <template #default="{ row }">
              <el-button v-perm="'org:user:update'" link type="primary" @click="openEdit(row as UserRow)">编辑/角色</el-button>
              <el-button v-perm="'org:user:reset_pwd'" link type="warning" @click="resetPwd(row as UserRow)">重置密码</el-button>
              <el-button v-perm="'org:user:disable'" link type="danger" @click="disableUser(row as UserRow)">停用</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div style="display:flex;justify-content:flex-end;margin-top:14px;">
          <el-pagination layout="total, prev, pager, next" :total="total"
            :page-size="query.size" :current-page="query.page"
            @current-change="(p:number) => { query.page = p; loadUsers() }" />
        </div>
      </div>
    </div>

    <el-dialog v-model="userDialog.visible" :title="userDialog.editId ? '编辑用户' : '新增用户'" width="480px">
      <el-form label-width="80px">
        <el-form-item label="登录名" v-if="!userDialog.editId">
          <el-input v-model="userForm.username" />
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="userForm.realName" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="userForm.mobile" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="userForm.email" /></el-form-item>
        <el-form-item label="部门">
          <el-tree-select v-model="userForm.deptId" :data="tree" :props="deptProps"
            node-key="id" check-strictly style="width:100%" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.roleIds" multiple style="width:100%" placeholder="选择角色">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
