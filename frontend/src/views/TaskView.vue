<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskApi } from '@/services/business'
import { useAuthStore } from '@/stores/auth'
import { userApi } from '@/services/system'
import type { TaskRow, UserRow } from '@/types'

const auth = useAuthStore()

const rows = ref<TaskRow[]>([])
const total = ref(0)
const loading = ref(false)
const users = ref<UserRow[]>([])

const query = reactive({
  view: 'mine',
  status: '',
  priority: '',
  keyword: '',
  page: 1,
  size: 20,
})

const views = computed(() => {
  const list = [
    { label: '我的任务', value: 'mine' },
    { label: '全部任务', value: 'all' },
  ]
  if (auth.hasPerm('task:view:sub')) {
    list.splice(1, 0, { label: '下属任务', value: 'sub' })
  }
  return list
})

const statusTabs = [
  { label: '全部', value: '' },
  { label: '待办', value: 'PENDING' },
  { label: '进行中', value: 'DOING' },
  { label: '已完成', value: 'DONE' },
  { label: '已逾期', value: 'OVERDUE' },
  { label: '已取消', value: 'CANCELLED' },
]

const statusLabels: Record<string, { label: string; type: 'primary' | 'success' | 'warning' | 'info' | 'danger' }> = {
  PENDING: { label: '待办', type: 'info' },
  DOING: { label: '进行中', type: 'primary' },
  DONE: { label: '已完成', type: 'success' },
  OVERDUE: { label: '已逾期', type: 'danger' },
  CANCELLED: { label: '已取消', type: 'warning' },
}

const priorityLabels: Record<string, { label: string; type: 'primary' | 'success' | 'warning' | 'info' | 'danger' }> = {
  HIGH: { label: '高', type: 'danger' },
  NORMAL: { label: '中', type: 'primary' },
  LOW: { label: '低', type: 'info' },
}

const objectTypeLabels: Record<string, string> = {
  opportunity: '商机', customer: '客户',
}

const userMap = computed(() => {
  const m: Record<number, string> = {}
  for (const u of users.value) m[u.id] = u.realName || u.username
  return m
})

function isOverdue(row: TaskRow): boolean {
  if (!row.dueAt || row.status === 'DONE' || row.status === 'CANCELLED') return false
  return new Date(row.dueAt) < new Date()
}

async function load() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { view: query.view, page: query.page, size: query.size }
    if (query.status) params.status = query.status
    if (query.priority) params.priority = query.priority
    if (query.keyword) params.keyword = query.keyword
    const data = await taskApi.list(params)
    rows.value = data.records
    total.value = data.total
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  try {
    const data = await userApi.list({ page: 1, size: 200 })
    users.value = data.records
  } catch { /* non-critical */ }
}

function onSearch() { query.page = 1; load() }

/* ---- 状态操作 ---- */
async function startTask(row: TaskRow) {
  try {
    const { default: http } = await import('@/services/request')
    await http.post(`/tasks/${row.id}/status`, null, { params: { status: 'DOING' } })
    ElMessage.success('已开始')
    load()
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '操作失败')
  }
}

async function completeTask(row: TaskRow) {
  try {
    // 直接调 status 接口
    const { default: http } = await import('@/services/request')
    await http.post(`/tasks/${row.id}/status`, null, { params: { status: 'DONE' } })
    ElMessage.success('已完成')
    load()
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '操作失败')
  }
}

async function cancelTask(row: TaskRow) {
  try {
    await ElMessageBox.confirm('确认取消此任务？', '取消任务', { type: 'warning' })
    await taskApi.cancel(row.id)
    ElMessage.success('已取消')
    load()
  } catch { /* user cancelled */ }
}

/* ---- 创建/编辑对话框 ---- */
const dialog = reactive({ visible: false, editId: 0 })
const form = reactive({
  title: '',
  content: '',
  objectType: '',
  objectId: null as number | null,
  assigneeId: null as number | null,
  priority: 'NORMAL',
  dueAt: '',
})

function openCreate() {
  dialog.visible = true
  dialog.editId = 0
  Object.assign(form, { title: '', content: '', objectType: '', objectId: null, assigneeId: null, priority: 'NORMAL', dueAt: '' })
}

async function submit() {
  if (!form.title) return ElMessage.warning('请输入任务标题')
  try {
    if (dialog.editId) {
      await taskApi.update(dialog.editId, { title: form.title, content: form.content, priority: form.priority, dueAt: form.dueAt })
    } else {
      await taskApi.create({ ...form })
    }
    ElMessage.success('已保存')
    dialog.visible = false
    load()
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '保存失败')
  }
}

onMounted(() => { load(); loadUsers() })
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="breadcrumb">业务 / 任务中心</div>
        <h1>任务中心</h1>
        <p>管理商机/客户关联任务，支持指派、优先级与状态流转（M12）。</p>
      </div>
      <div class="head-actions">
        <el-button v-perm="'task:create'" type="primary" @click="openCreate">新建任务</el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-card">
      <el-radio-group v-model="query.view" @change="onSearch" size="small">
        <el-radio-button v-for="v in views" :key="v.value" :value="v.value">{{ v.label }}</el-radio-button>
      </el-radio-group>
      <el-select v-model="query.status" placeholder="状态" style="width:120px" clearable @change="onSearch">
        <el-option v-for="s in statusTabs" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-select v-model="query.priority" placeholder="优先级" style="width:110px" clearable @change="onSearch">
        <el-option label="高" value="HIGH" />
        <el-option label="中" value="NORMAL" />
        <el-option label="低" value="LOW" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="搜索标题" style="width:180px" clearable @keyup.enter="onSearch" />
      <el-button type="primary" @click="onSearch">查询</el-button>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="title" label="任务" min-width="200" />
        <el-table-column label="关联" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.objectType" size="small">{{ objectTypeLabels[row.objectType] || row.objectType }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="负责人" width="100">
          <template #default="{ row }">{{ userMap[row.assigneeId!] || row.assigneeId || '-' }}</template>
        </el-table-column>
        <el-table-column label="优先级" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="priorityLabels[row.priority]?.type || 'info'">
              {{ priorityLabels[row.priority]?.label || row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusLabels[row.status]?.type || 'info'">
              {{ statusLabels[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="截止日期" width="120">
          <template #default="{ row }">
            <span :style="{ color: isOverdue(row as TaskRow) ? 'var(--red)' : '' }">
              {{ row.dueAt ? row.dueAt.substring(0, 10) : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING' || row.status === 'OVERDUE'" v-perm="'task:update'" link type="primary" size="small" @click="startTask(row as TaskRow)">开始</el-button>
            <el-button v-if="row.status === 'DOING' || row.status === 'OVERDUE'" v-perm="'task:update'" link type="success" size="small" @click="completeTask(row as TaskRow)">完成</el-button>
            <el-button v-if="row.status !== 'DONE' && row.status !== 'CANCELLED'" v-perm="'task:cancel'" link type="danger" size="small" @click="cancelTask(row as TaskRow)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display:flex;justify-content:flex-end;margin-top:16px">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="onSearch"
        />
      </div>
    </div>

    <!-- 新建对话框 -->
    <el-dialog v-model="dialog.visible" :title="dialog.editId ? '编辑任务' : '新建任务'" width="520px">
      <el-form label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="任务标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.content" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="关联类型">
          <el-select v-model="form.objectType" clearable style="width:100%">
            <el-option label="商机" value="opportunity" />
            <el-option label="客户" value="customer" />
          </el-select>
        </el-form-item>
        <el-form-item label="指派给">
          <el-select v-model="form.assigneeId" clearable filterable style="width:100%">
            <el-option v-for="u in users" :key="u.id" :label="u.realName || u.username" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" style="width:100%">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="NORMAL" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="form.dueAt" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
