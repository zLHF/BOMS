<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { configApi } from '@/services/business'
import type { StageRow, NumberRuleRow } from '@/types'

const stages = ref<StageRow[]>([])
const rules = ref<NumberRuleRow[]>([])
const loading = ref(false)

/* ---------- 阶段编辑 ---------- */
const editingStage = reactive<Record<number, { name: string; winRate: number; color: string; isActive: number }>>({})
const savingStage = ref<number | null>(null)

function startEditStage(row: StageRow) {
  editingStage[row.id] = { name: row.name, winRate: row.winRate, color: row.color ?? '', isActive: row.isActive ?? 1 }
}
function cancelEditStage(id: number) {
  delete editingStage[id]
}
async function saveStage(id: number) {
  const d = editingStage[id]
  if (!d) return
  savingStage.value = id
  try {
    await configApi.updateStage(id, d)
    ElMessage.success('阶段已更新')
    delete editingStage[id]
    load()
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '保存失败')
  } finally {
    savingStage.value = null
  }
}

const stageTypeLabels: Record<string, string> = {
  IN_PROGRESS: '进行中', WON: '赢单', LOST: '输单', VOID: '作废',
}

const colorOptions = [
  { label: '灰', value: 'gray' },
  { label: '青', value: 'cyan' },
  { label: '蓝', value: 'blue' },
  { label: '橙', value: 'orange' },
  { label: '绿', value: 'green' },
  { label: '红', value: 'red' },
  { label: '紫', value: 'purple' },
]

/* ---------- 编号规则编辑 ---------- */
const editingRule = reactive<Record<string, { prefix: string; dateFormat: string; seqLength: number }>>({})
const savingRule = ref<string | null>(null)

function startEditRule(row: NumberRuleRow) {
  editingRule[row.bizType] = {
    prefix: row.prefix ?? '',
    dateFormat: row.dateFormat ?? '',
    seqLength: row.seqLength,
  }
}
function cancelEditRule(bizType: string) {
  delete editingRule[bizType]
}
async function saveRule(bizType: string) {
  const d = editingRule[bizType]
  if (!d) return
  savingRule.value = bizType
  try {
    await configApi.updateNumberRule(bizType, d)
    ElMessage.success('编号规则已更新')
    delete editingRule[bizType]
    load()
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '保存失败')
  } finally {
    savingRule.value = null
  }
}

const bizTypeLabels: Record<string, string> = {
  opportunity: '商机', customer: '客户',
}

async function load() {
  loading.value = true
  try {
    const [s, r] = await Promise.all([configApi.stages(), configApi.numberRules()])
    stages.value = s
    rules.value = r
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="breadcrumb">设置 / 阶段 / 编号配置</div>
        <h1>阶段 / 编号配置</h1>
        <p>配置商机阶段属性与业务编号生成规则（M19）。</p>
      </div>
    </div>

    <!-- 阶段配置 -->
    <div class="panel" style="margin-bottom:24px">
      <h3 style="margin:0 0 16px" v-perm="'config:stage'">阶段配置</h3>
      <el-table :data="stages" v-loading="loading" stripe row-key="id">
        <el-table-column prop="code" label="编码" width="140" />
        <el-table-column label="名称" width="140">
          <template #default="{ row }">
            <template v-if="editingStage[row.id]">
              <el-input v-model="editingStage[row.id].name" size="small" />
            </template>
            <template v-else>{{ row.name }}</template>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="赢率%" width="110">
          <template #default="{ row }">
            <template v-if="editingStage[row.id]">
              <el-input-number v-model="editingStage[row.id].winRate" :min="0" :max="100" size="small" style="width:90px" />
            </template>
            <template v-else>{{ row.winRate }}</template>
          </template>
        </el-table-column>
        <el-table-column label="颜色" width="120">
          <template #default="{ row }">
            <template v-if="editingStage[row.id]">
              <el-select v-model="editingStage[row.id].color" size="small" style="width:90px">
                <el-option v-for="c in colorOptions" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </template>
            <template v-else>
              <el-tag size="small" :type="(row.color || 'info') as any">{{ row.color || '-' }}</el-tag>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ stageTypeLabels[row.stageType] || row.stageType }}</template>
        </el-table-column>
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <template v-if="editingStage[row.id]">
              <el-switch v-model="editingStage[row.id].isActive" :active-value="1" :inactive-value="0" size="small" />
            </template>
            <template v-else>
              <el-tag size="small" :type="row.isActive ? 'success' : 'info'">{{ row.isActive ? '是' : '否' }}</el-tag>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <template v-if="editingStage[row.id]">
              <el-button link type="primary" size="small" :loading="savingStage === row.id" @click="saveStage(row.id)">保存</el-button>
              <el-button link size="small" @click="cancelEditStage(row.id)">取消</el-button>
            </template>
            <template v-else>
              <el-button v-perm="'config:stage'" link type="primary" size="small" @click="startEditStage(row as StageRow)">编辑</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 编号规则 -->
    <div class="panel">
      <h3 style="margin:0 0 16px" v-perm="'config:number'">编号规则</h3>
      <el-table :data="rules" v-loading="loading" stripe row-key="id">
        <el-table-column label="业务类型" width="120">
          <template #default="{ row }">{{ bizTypeLabels[row.bizType] || row.bizType }}</template>
        </el-table-column>
        <el-table-column label="前缀" width="140">
          <template #default="{ row }">
            <template v-if="editingRule[row.bizType]">
              <el-input v-model="editingRule[row.bizType].prefix" size="small" placeholder="如 OPP" />
            </template>
            <template v-else>{{ row.prefix || '-' }}</template>
          </template>
        </el-table-column>
        <el-table-column label="日期格式" width="160">
          <template #default="{ row }">
            <template v-if="editingRule[row.bizType]">
              <el-input v-model="editingRule[row.bizType].dateFormat" size="small" placeholder="如 yyyyMMdd" />
            </template>
            <template v-else>{{ row.dateFormat || '-' }}</template>
          </template>
        </el-table-column>
        <el-table-column label="流水位数" width="120">
          <template #default="{ row }">
            <template v-if="editingRule[row.bizType]">
              <el-input-number v-model="editingRule[row.bizType].seqLength" :min="1" :max="10" size="small" style="width:100px" />
            </template>
            <template v-else>{{ row.seqLength }}</template>
          </template>
        </el-table-column>
        <el-table-column prop="currentSeq" label="当前流水" width="100" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <template v-if="editingRule[row.bizType]">
              <el-button link type="primary" size="small" :loading="savingRule === row.bizType" @click="saveRule(row.bizType)">保存</el-button>
              <el-button link size="small" @click="cancelEditRule(row.bizType)">取消</el-button>
            </template>
            <template v-else>
              <el-button v-perm="'config:number'" link type="primary" size="small" @click="startEditRule(row as NumberRuleRow)">编辑</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
