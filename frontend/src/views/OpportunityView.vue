<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { oppApi, customerApi, configApi, poolApi } from '@/services/business'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import type { OpportunityRow, CustomerRow, StageRow, FollowRow } from '@/types'

const auth = useAuthStore()
const router = useRouter()
const rows = ref<OpportunityRow[]>([])
const total = ref(0)
const loading = ref(false)
const stages = ref<StageRow[]>([])
const customers = ref<CustomerRow[]>([])

const query = reactive({ view: 'all', keyword: '', stageId: undefined as number | undefined, page: 1, size: 20 })

const views = computed(() => {
  const base = [
    { key: 'all', label: '全部' },
    { key: 'mine', label: '我的' },
  ]
  if (auth.hasPerm('opp:view:sub')) base.push({ key: 'sub', label: '下属' })
  base.push({ key: 'won', label: '已成交' })
  base.push({ key: 'pool', label: '公海' })
  return base
})

const stageMap = computed(() => Object.fromEntries(stages.value.map((s) => [s.id, s])))
const custMap = computed(() => Object.fromEntries(customers.value.map((c) => [c.id, c.name])))
const inProgressStages = computed(() => stages.value.filter((s) => s.stageType === 'IN_PROGRESS'))

const statusMeta: Record<string, { label: string; type: string }> = {
  IN_PROGRESS: { label: '进行中', type: 'primary' },
  WON: { label: '赢单', type: 'success' },
  LOST: { label: '输单', type: 'danger' },
  VOID: { label: '作废', type: 'info' },
}

async function load() {
  loading.value = true
  try {
    const p = await oppApi.list({ ...query })
    rows.value = p.records
    total.value = p.total
  } finally {
    loading.value = false
  }
}
function switchView(v: string) {
  query.view = v; query.page = 1; load()
}

/* ---------- 公海操作 ---------- */
async function claimOpp(row: OpportunityRow) {
  try {
    await poolApi.claimOpportunity(row.id)
    ElMessage.success('认领成功')
    load()
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '认领失败')
  }
}

async function recycleOpp(row: OpportunityRow) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入回收原因（可选）', '回收到公海', {
      confirmButtonText: '确认回收', cancelButtonText: '取消', inputPlaceholder: '回收原因',
    }).catch(() => ({ value: '' }))
    await poolApi.recycleOpportunity(row.id, reason || '')
    ElMessage.success('已回收到公海')
    load()
  } catch (e: unknown) {
    if ((e as string) !== 'cancel') ElMessage.error((e as Error).message || '回收失败')
  }
}

async function loadMeta() {
  stages.value = await configApi.stages()
  customers.value = (await customerApi.list({ size: 200 })).records
}

/* 新建商机 */
const dialog = reactive({ visible: false })
const form = reactive({ title: '', customerId: undefined as number | undefined, amount: 0, source: '', demand: '', expectedCloseAt: '' })
function openCreate() {
  dialog.visible = true
  Object.assign(form, { title: '', customerId: undefined, amount: 0, source: '', demand: '', expectedCloseAt: '' })
}
async function submit() {
  if (!form.title || !form.customerId) return ElMessage.warning('请填写标题并选择客户')
  await oppApi.create({ ...form })
  ElMessage.success('已创建'); dialog.visible = false; load()
}

/* 详情抽屉 */
const drawer = reactive({ visible: false, id: 0 })
const detail = ref<OpportunityRow | null>(null)
const follows = ref<FollowRow[]>([])
const followForm = reactive({ followType: '电话', content: '', nextTime: '' })

async function openDetail(row: OpportunityRow) {
  drawer.visible = true; drawer.id = row.id
  detail.value = await oppApi.get(row.id)
  follows.value = await oppApi.follows(row.id)
  Object.assign(followForm, { followType: '电话', content: '', nextTime: '' })
}
async function refreshDetail() {
  detail.value = await oppApi.get(drawer.id)
  follows.value = await oppApi.follows(drawer.id)
  load()
}
async function moveStage(stageId: number) {
  await oppApi.moveStage(drawer.id, stageId)
  ElMessage.success('阶段已推进'); refreshDetail()
}
async function addFollow() {
  if (!followForm.content) return ElMessage.warning('请填写跟进内容')
  await oppApi.addFollow(drawer.id, { ...followForm })
  ElMessage.success('已记录'); refreshDetail()
  Object.assign(followForm, { followType: '电话', content: '', nextTime: '' })
}
async function doWin() {
  const { value } = await ElMessageBox.prompt('成交金额（必填）', '成交', { inputPattern: /\d+/, inputErrorMessage: '请输入金额' })
  await oppApi.win(drawer.id, { dealAmount: Number(value), dealAt: new Date().toISOString().slice(0, 10), note: '前端快速成交' })
  ElMessage.success('已成交'); refreshDetail()
}
async function doLose() {
  const { value } = await ElMessageBox.prompt('输单原因（必填）', '输单', { inputErrorMessage: '请输入原因' })
  await oppApi.lose(drawer.id, { reason: value, competitor: '', review: '' })
  ElMessage.success('已记录输单'); refreshDetail()
}

onMounted(() => { loadMeta(); load() })
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="breadcrumb">业务 / 商机管理</div>
        <h1>商机管理</h1>
        <p>多视图列表 + 阶段流转（M05/M06/M07）。列表按数据范围过滤，阶段链/成交规则见 D1。</p>
      </div>
      <div class="head-actions">
        <el-button v-perm="'opp:create'" type="primary" @click="openCreate">新增商机</el-button>
      </div>
    </div>

    <div class="filter-card compact" style="gap:8px;">
      <el-radio-group :model-value="query.view" @change="(v:any) => switchView(v)">
        <el-radio-button v-for="v in views" :key="v.key" :value="v.key">{{ v.label }}</el-radio-button>
      </el-radio-group>
      <el-input v-model="query.keyword" placeholder="商机标题" clearable style="max-width:200px" @keyup.enter="() => { query.page = 1; load() }" />
      <el-select v-model="query.stageId" placeholder="阶段" clearable style="width:130px" @change="() => { query.page = 1; load() }">
        <el-option v-for="s in stages" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-button @click="() => { query.page = 1; load() }">查询</el-button>
    </div>

    <div class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="title" label="商机标题" min-width="180" />
        <el-table-column label="客户" width="150">
          <template #default="{ row }">{{ custMap[row.customerId] || ('#' + row.customerId) }}</template>
        </el-table-column>
        <el-table-column label="阶段" width="110">
          <template #default="{ row }">
            <span class="stage" :class="stageMap[row.stageId]?.color || 'gray'">{{ stageMap[row.stageId]?.name || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }">¥{{ Number(row.amount).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="赢率" width="80">
          <template #default="{ row }">{{ row.winRate }}%</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="(statusMeta[row.status]?.type as any)" size="small">{{ statusMeta[row.status]?.label || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push('/opportunity/' + row.id)">详情</el-button>
            <el-button v-if="query.view === 'pool'" v-perm="'opp:create'" link type="success" @click="claimOpp(row as OpportunityRow)">认领</el-button>
            <el-button v-if="query.view !== 'pool'" v-perm="'opp:transfer'" link type="warning" @click="recycleOpp(row as OpportunityRow)">回收</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:14px;">
        <el-pagination layout="total, prev, pager, next" :total="total" :page-size="query.size"
          :current-page="query.page" @current-change="(p:number) => { query.page = p; load() }" />
      </div>
    </div>

    <!-- 新建商机 -->
    <el-dialog v-model="dialog.visible" title="新增商机" width="500px">
      <el-form label-width="90px">
        <el-form-item label="商机标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="关联客户">
          <el-select v-model="form.customerId" filterable placeholder="选择客户" style="width:100%">
            <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预计金额"><el-input-number v-model="form.amount" :min="0" :step="10000" style="width:100%" /></el-form-item>
        <el-form-item label="来源"><el-input v-model="form.source" /></el-form-item>
        <el-form-item label="客户需求"><el-input v-model="form.demand" type="textarea" /></el-form-item>
        <el-form-item label="预计成交">
          <el-date-picker v-model="form.expectedCloseAt" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submit">创建</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="drawer.visible" size="560px" :title="detail?.title || '商机详情'">
      <template v-if="detail">
        <!-- 阶段条 -->
        <div class="stage-bar" :style="{ gridTemplateColumns: `repeat(${inProgressStages.length}, 1fr)` }">
          <span v-for="s in inProgressStages" :key="s.id"
            :class="{ active: s.id === detail.stageId }">{{ s.name }}</span>
        </div>

        <div class="info-grid" style="margin:16px 0;">
          <div><span>客户</span><b>{{ custMap[detail.customerId] || ('#' + detail.customerId) }}</b></div>
          <div><span>金额 / 赢率</span><b>¥{{ Number(detail.amount).toLocaleString() }} · {{ detail.winRate }}%</b></div>
          <div><span>状态</span><b>{{ statusMeta[detail.status]?.label }}</b></div>
          <div><span>成交金额</span><b>{{ detail.dealAmount ? '¥' + Number(detail.dealAmount).toLocaleString() : '-' }}</b></div>
        </div>

        <div class="drawer-actions" style="grid-template-columns:repeat(2,1fr);" v-if="detail.status === 'IN_PROGRESS'">
          <el-select placeholder="推进到阶段" v-perm="'opp:stage:advance'" @change="(v:number) => moveStage(v)">
            <el-option v-for="s in inProgressStages" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <div>
            <el-button v-perm="'opp:win'" type="success" @click="doWin">成交</el-button>
            <el-button v-perm="'opp:lose'" type="danger" @click="doLose">输单</el-button>
          </div>
        </div>

        <!-- 跟进 -->
        <div class="timeline">
          <h4>跟进记录（{{ follows.length }}）</h4>
          <div v-perm="'opp:follow:create'" class="compact" style="box-shadow:none;padding:0;margin-bottom:12px;gap:6px;">
            <el-input v-model="followForm.content" placeholder="记录一次跟进…" @keyup.enter="addFollow" />
            <el-button type="primary" @click="addFollow">记录</el-button>
          </div>
          <div v-for="f in follows" :key="f.id" class="time-item" style="margin-bottom:12px;">
            <i></i>
            <p><b>{{ f.followType || '跟进' }}</b>　{{ f.content }}</p>
            <p style="color:var(--muted);font-size:12px;">{{ f.createdAt }}</p>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>
