<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { customerApi, poolApi } from '@/services/business'
import type { CustomerRow, ContactRow } from '@/types'

const rows = ref<CustomerRow[]>([])
const total = ref(0)
const loading = ref(false)
const poolView = ref(false)
const query = reactive({ keyword: '', level: '', page: 1, size: 20 })

async function load() {
  loading.value = true
  try {
    if (poolView.value) {
      const p = await poolApi.customers({ ...query })
      rows.value = p.records
      total.value = p.total
    } else {
      const p = await customerApi.list({ ...query })
      rows.value = p.records
      total.value = p.total
    }
  } finally {
    loading.value = false
  }
}

function togglePool() {
  poolView.value = !poolView.value
  query.page = 1
  load()
}

/* 新建/编辑 */
const dialog = reactive({ visible: false, editId: 0 })
const form = reactive({ name: '', creditCode: '', industry: '', region: '', level: '', force: false })
function openCreate() {
  dialog.visible = true; dialog.editId = 0
  Object.assign(form, { name: '', creditCode: '', industry: '', region: '', level: '', force: false })
}
function openEdit(row: CustomerRow) {
  dialog.visible = true; dialog.editId = row.id
  Object.assign(form, { name: row.name, creditCode: row.creditCode ?? '', industry: row.industry ?? '', region: row.region ?? '', level: row.level ?? '', force: false })
}
async function submit() {
  if (!form.name) return ElMessage.warning('请填写客户名称')
  try {
    if (dialog.editId) await customerApi.update(dialog.editId, { ...form })
    else await customerApi.create({ ...form })
    ElMessage.success('已保存'); dialog.visible = false; load()
  } catch (e) {
    const err = e as Error & { code?: number }
    if (err.code === 40910) {
      await ElMessageBox.confirm(err.message + '，仍要创建吗？', '疑似重复', { type: 'warning' })
      await customerApi.create({ ...form, force: true })
      ElMessage.success('已创建'); dialog.visible = false; load()
    }
  }
}
async function remove(row: CustomerRow) {
  await ElMessageBox.confirm(`删除客户「${row.name}」？`, '确认', { type: 'warning' })
  await customerApi.remove(row.id); ElMessage.success('已删除'); load()
}

/* 公海操作 */
async function claimCustomer(row: CustomerRow) {
  try {
    await poolApi.claimCustomer(row.id)
    ElMessage.success('认领成功')
    load()
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '认领失败')
  }
}

async function recycleCustomer(row: CustomerRow) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入回收原因（可选）', '回收到公海', {
      confirmButtonText: '确认回收', cancelButtonText: '取消', inputPlaceholder: '回收原因',
    }).catch(() => ({ value: '' }))
    await poolApi.recycleCustomer(row.id, reason || '')
    ElMessage.success('已回收到公海')
    load()
  } catch (e: unknown) {
    if ((e as string) !== 'cancel') ElMessage.error((e as Error).message || '回收失败')
  }
}

/* 联系人抽屉 */
const drawer = reactive({ visible: false, customerId: 0, customerName: '' })
const contacts = ref<ContactRow[]>([])
const contactForm = reactive({ name: '', title: '', mobile: '', keyPerson: false })
async function openContacts(row: CustomerRow) {
  drawer.visible = true; drawer.customerId = row.id; drawer.customerName = row.name
  Object.assign(contactForm, { name: '', title: '', mobile: '', keyPerson: false })
  contacts.value = await customerApi.contacts(row.id)
}
async function addContact() {
  if (!contactForm.name) return ElMessage.warning('请填写联系人姓名')
  await customerApi.addContact(drawer.customerId, { ...contactForm })
  ElMessage.success('已添加')
  contacts.value = await customerApi.contacts(drawer.customerId)
  Object.assign(contactForm, { name: '', title: '', mobile: '', keyPerson: false })
}
async function delContact(c: ContactRow) {
  await customerApi.delContact(drawer.customerId, c.id)
  contacts.value = await customerApi.contacts(drawer.customerId)
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="breadcrumb">业务 / 客户管理</div>
        <h1>客户管理</h1>
        <p>客户列表 + 联系人（M10）。列表按数据范围过滤；信用代码租户内唯一，同名软提示（D2）。</p>
      </div>
      <div class="head-actions">
        <el-button :type="poolView ? 'warning' : ''" @click="togglePool">{{ poolView ? '返回客户列表' : '公海池' }}</el-button>
        <el-button v-if="!poolView" v-perm="'customer:create'" type="primary" @click="openCreate">新增客户</el-button>
      </div>
    </div>

    <div class="filter-card compact">
      <el-input v-model="query.keyword" placeholder="客户名称" clearable style="max-width:240px" @keyup.enter="() => { query.page = 1; load() }" />
      <el-select v-model="query.level" placeholder="等级" clearable style="width:120px">
        <el-option label="A" value="A" /><el-option label="B" value="B" /><el-option label="C" value="C" />
      </el-select>
      <el-button @click="() => { query.page = 1; load() }">查询</el-button>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="name" label="客户名称" min-width="160" />
        <el-table-column prop="creditCode" label="信用代码" width="180" />
        <el-table-column prop="industry" label="行业" width="110" />
        <el-table-column prop="region" label="地区" width="110" />
        <el-table-column label="等级" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.level" size="small" :type="row.level === 'A' ? 'danger' : (row.level === 'B' ? 'warning' : 'info')">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <template v-if="poolView">
              <el-button v-perm="'customer:create'" link type="success" @click="claimCustomer(row as CustomerRow)">认领</el-button>
            </template>
            <template v-else>
              <el-button link type="primary" @click="openContacts(row as CustomerRow)">联系人</el-button>
              <el-button v-perm="'customer:update'" link type="primary" @click="openEdit(row as CustomerRow)">编辑</el-button>
              <el-button v-perm="'customer:update'" link type="danger" @click="remove(row as CustomerRow)">删除</el-button>
              <el-button v-perm="'customer:transfer'" link type="warning" @click="recycleCustomer(row as CustomerRow)">回收</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:14px;">
        <el-pagination layout="total, prev, pager, next" :total="total" :page-size="query.size"
          :current-page="query.page" @current-change="(p:number) => { query.page = p; load() }" />
      </div>
    </div>

    <el-dialog v-model="dialog.visible" :title="dialog.editId ? '编辑客户' : '新增客户'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="客户名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="信用代码"><el-input v-model="form.creditCode" placeholder="填则租户内唯一" /></el-form-item>
        <el-form-item label="行业"><el-input v-model="form.industry" /></el-form-item>
        <el-form-item label="地区"><el-input v-model="form.region" /></el-form-item>
        <el-form-item label="等级">
          <el-select v-model="form.level" clearable style="width:100%">
            <el-option label="A" value="A" /><el-option label="B" value="B" /><el-option label="C" value="C" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawer.visible" :title="`联系人 · ${drawer.customerName}`" size="420px">
      <div v-for="c in contacts" :key="c.id" class="contact-card" style="margin-bottom:10px;">
        <div style="flex:1">
          <b>{{ c.name }}</b> <el-tag v-if="c.isKeyPerson" size="small" type="danger">关键</el-tag>
          <p>{{ c.title || '-' }}　{{ c.mobile || '' }}</p>
        </div>
        <el-button v-perm="'customer:contact:manage'" link type="danger" @click="delContact(c)">删除</el-button>
      </div>
      <el-divider>新增联系人</el-divider>
      <el-form label-width="70px">
        <el-form-item label="姓名"><el-input v-model="contactForm.name" /></el-form-item>
        <el-form-item label="职务"><el-input v-model="contactForm.title" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="contactForm.mobile" /></el-form-item>
        <el-form-item label="关键人"><el-switch v-model="contactForm.keyPerson" /></el-form-item>
        <el-button v-perm="'customer:contact:manage'" type="primary" @click="addContact">添加联系人</el-button>
      </el-form>
    </el-drawer>
  </div>
</template>
