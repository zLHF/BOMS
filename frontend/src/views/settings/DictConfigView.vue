<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { dictApi } from '@/services/system'
import type { DictRow, Page } from '@/types'

const loading = ref(false)
const rows = ref<DictRow[]>([])
const total = ref(0)
const dictTypes = ref<string[]>([])
const filterType = ref('')

const dialog = reactive({ visible: false, isEdit: false, editId: 0 })
const form = reactive({
  dictType: '',
  itemCode: '',
  itemLabel: '',
  sort: 0,
  isActive: 1,
})

async function loadTypes() {
  try {
    dictTypes.value = await dictApi.listTypes()
  } catch { /* */ }
}

async function load() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: 1, size: 200 }
    if (filterType.value) params.dictType = filterType.value
    const data: Page<DictRow> = await dictApi.listPage(params)
    rows.value = data.records
    total.value = data.total
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialog.isEdit = false
  dialog.editId = 0
  Object.assign(form, { dictType: filterType.value || '', itemCode: '', itemLabel: '', sort: rows.value.length + 1, isActive: 1 })
  dialog.visible = true
}

function openEdit(row: DictRow) {
  dialog.isEdit = true
  dialog.editId = row.id
  Object.assign(form, { dictType: row.dictType, itemCode: row.itemCode, itemLabel: row.itemLabel, sort: row.sort, isActive: row.isActive })
  dialog.visible = true
}

async function submitForm() {
  if (!form.dictType || !form.itemCode || !form.itemLabel) {
    return ElMessage.warning('请填写完整信息')
  }
  try {
    if (dialog.isEdit) {
      await dictApi.update(dialog.editId, form)
      ElMessage.success('已更新')
    } else {
      await dictApi.create(form)
      ElMessage.success('已创建')
    }
    dialog.visible = false
    load()
    loadTypes()
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '操作失败')
  }
}

async function removeRow(row: DictRow) {
  try {
    await ElMessageBox.confirm(`确认删除「${row.itemLabel}」？`, '确认删除', { type: 'warning' })
    await dictApi.remove(row.id)
    ElMessage.success('已删除')
    load()
    loadTypes()
  } catch { /* user cancelled */ }
}

onMounted(() => { load(); loadTypes() })
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="breadcrumb">设置 / 数据字典</div>
        <h1>数据字典</h1>
        <p>管理系统中各下拉选项的字典数据，如客户行业、商机来源、跟进方式等。</p>
      </div>
    </div>

    <div class="panel">
      <!-- 筛选 + 操作 -->
      <div class="filter-bar">
        <el-select v-model="filterType" placeholder="全部类型" clearable style="width:200px" @change="load">
          <el-option v-for="t in dictTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增字典项</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="rows" v-loading="loading" stripe style="margin-top:12px">
        <el-table-column prop="dictType" label="字典类型" width="180" />
        <el-table-column prop="itemCode" label="编码" width="140" />
        <el-table-column prop="itemLabel" label="显示名称" min-width="160" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.isActive ? 'success' : 'info'">{{ row.isActive ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row as DictRow)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="removeRow(row as DictRow)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑字典项' : '新增字典项'" width="480px">
      <el-form label-width="100px">
        <el-form-item label="字典类型" required>
          <el-select v-model="form.dictType" filterable allow-create style="width:100%" placeholder="选择或输入新类型">
            <el-option v-for="t in dictTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="编码" required><el-input v-model="form.itemCode" placeholder="如 IT / website" /></el-form-item>
        <el-form-item label="显示名称" required><el-input v-model="form.itemLabel" placeholder="如 IT/互联网 / 官网咨询" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.isActive" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">{{ dialog.isEdit ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
