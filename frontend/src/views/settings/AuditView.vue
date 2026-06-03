<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { auditApi } from '@/services/business'
import type { AuditLogRow } from '@/types'

const rows = ref<AuditLogRow[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  objectType: '' as string,
  action: '',
  start: '',
  end: '',
  page: 1,
  size: 20,
})

const objectTypes = [
  { label: '全部', value: '' },
  { label: '商机', value: 'opportunity' },
  { label: '客户', value: 'customer' },
  { label: '任务', value: 'task' },
  { label: '联系人', value: 'contact' },
  { label: '用户', value: 'user' },
  { label: '角色', value: 'role' },
  { label: '部门', value: 'department' },
  { label: '租户', value: 'tenant' },
  { label: '配置', value: 'config' },
]

/* 展开行：显示变更前后 JSON */
const expandRow = ref<number | null>(null)
function toggleExpand(id: number) {
  expandRow.value = expandRow.value === id ? null : id
}

function formatJson(json?: string): string {
  if (!json) return '-'
  try { return JSON.stringify(JSON.parse(json), null, 2) }
  catch { return json }
}

const resultLabels: Record<string, { label: string; type: 'primary' | 'success' | 'warning' | 'info' | 'danger' }> = {
  SUCCESS: { label: '成功', type: 'success' },
  FAILURE: { label: '失败', type: 'danger' },
}

async function load() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: query.page, size: query.size }
    if (query.objectType) params.objectType = query.objectType
    if (query.action) params.action = query.action
    if (query.start) params.start = query.start
    if (query.end) params.end = query.end
    const data = await auditApi.list(params)
    rows.value = data.records
    total.value = data.total
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onSearch() { query.page = 1; load() }

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="breadcrumb">设置 / 操作日志</div>
        <h1>操作日志</h1>
        <p>查看系统操作审计记录（M16，租户内只读）。</p>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-card">
      <el-select v-model="query.objectType" placeholder="对象类型" style="width:140px" clearable>
        <el-option v-for="t in objectTypes" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-input v-model="query.action" placeholder="操作码" style="width:180px" clearable />
      <el-date-picker
        v-model="query.start" type="date" value-format="YYYY-MM-DD"
        placeholder="开始日期" style="width:150px" clearable
      />
      <el-date-picker
        v-model="query.end" type="date" value-format="YYYY-MM-DD"
        placeholder="结束日期" style="width:150px" clearable
      />
      <el-button type="primary" @click="onSearch">查询</el-button>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe row-key="id">
        <el-table-column prop="userName" label="操作人" width="100" />
        <el-table-column prop="objectType" label="对象类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.objectType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="objectId" label="对象ID" width="80" />
        <el-table-column prop="action" label="操作" min-width="160" />
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="resultLabels[row.result]?.type || 'info'">
              {{ resultLabels[row.result]?.label || row.result }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column label="变更" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="toggleExpand(row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 展开的变更详情 -->
      <el-dialog v-model="!!expandRow" title="变更详情" width="680px" @close="expandRow = null">
        <template v-if="expandRow">
          <div v-for="row in rows.filter(r => r.id === expandRow)" :key="row.id" style="display:flex;gap:16px;">
            <div style="flex:1">
              <h4 style="margin:0 0 8px;color:var(--red)">变更前</h4>
              <pre style="background:#fef2f2;padding:12px;border-radius:8px;font-size:12px;max-height:360px;overflow:auto">{{ formatJson(row.beforeJson) }}</pre>
            </div>
            <div style="flex:1">
              <h4 style="margin:0 0 8px;color:var(--green)">变更后</h4>
              <pre style="background:#f0fdf4;padding:12px;border-radius:8px;font-size:12px;max-height:360px;overflow:auto">{{ formatJson(row.afterJson) }}</pre>
            </div>
          </div>
        </template>
      </el-dialog>

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
  </div>
</template>
