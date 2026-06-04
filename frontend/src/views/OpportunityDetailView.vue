<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { oppApi, configApi, auditApi, taskApi } from '@/services/business'
import { userApi } from '@/services/system'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import type { OpportunityDetailVO, StageRow, ContactRow, FollowRow, CollaboratorRow, AuditLogRow, TaskRow, UserRow } from '@/types'

const route = useRoute()
const router = useRouter()
const oppId = computed(() => Number(route.params.id))

const detail = ref<OpportunityDetailVO | null>(null)
const stages = ref<StageRow[]>([])
const activeTab = ref('overview')
const users = ref<UserRow[]>([])
const loading = ref(false)

const statusLabels: Record<string, { label: string; type: 'primary' | 'success' | 'warning' | 'info' | 'danger' }> = {
  IN_PROGRESS: { label: '进行中', type: 'primary' },
  WON: { label: '赢单', type: 'success' },
  LOST: { label: '输单', type: 'danger' },
  VOID: { label: '作废', type: 'info' },
}

const inProgressStages = computed(() => stages.value.filter(s => s.stageType === 'IN_PROGRESS'))

/* ---------- 加载基础数据 ---------- */
async function loadBase() {
  loading.value = true
  try {
    const [d, s] = await Promise.all([
      oppApi.detail(oppId.value),
      configApi.stages(),
    ])
    detail.value = d
    stages.value = s
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

const userMap = computed(() => {
  const m: Record<number, string> = {}
  for (const u of users.value) m[u.id] = u.realName || u.username
  return m
})

/* ---------- Tab 1: 概况 - 阶段操作 ---------- */
async function moveStage(stageId: number) {
  try {
    await oppApi.moveStage(oppId.value, stageId)
    ElMessage.success('阶段已推进')
    loadBase()
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '操作失败')
  }
}

async function doWin() {
  try {
    const { value: amount } = await ElMessageBox.prompt('请输入成交金额', '赢单', { inputPattern: /^\d+(\.\d+)?$/, inputErrorMessage: '请输入有效金额' })
    await oppApi.win(oppId.value, { dealAmount: Number(amount), dealAt: new Date().toISOString().substring(0, 10) })
    ElMessage.success('已标记赢单')
    loadBase()
  } catch { /* user cancelled */ }
}

async function doLose() {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入输单原因', '输单', {})
    await oppApi.lose(oppId.value, { reason })
    ElMessage.success('已标记输单')
    loadBase()
  } catch { /* user cancelled */ }
}

/* ---------- Tab 2: 联系人 ---------- */
const contacts = ref<ContactRow[]>([])
async function loadContacts() {
  try { contacts.value = await oppApi.contacts(oppId.value) }
  catch { /* */ }
}

/* ---------- Tab 3: 跟进 ---------- */
const follows = ref<FollowRow[]>([])
const followForm = reactive({ followType: '电话', content: '', result: '', nextTime: '' })
async function loadFollows() {
  try { follows.value = await oppApi.follows(oppId.value) }
  catch { /* */ }
}
async function addFollow() {
  if (!followForm.content) return ElMessage.warning('请填写跟进内容')
  try {
    await oppApi.addFollow(oppId.value, { ...followForm })
    ElMessage.success('跟进已添加')
    Object.assign(followForm, { followType: '电话', content: '', result: '', nextTime: '' })
    loadFollows()
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '添加失败')
  }
}

/* ---------- Tab 4: 任务 ---------- */
const tasks = ref<TaskRow[]>([])
const taskDialog = reactive({ visible: false })
const taskForm = reactive({ title: '', content: '', assigneeId: null as number | null, priority: 'NORMAL', dueAt: '' })

async function loadTasks() {
  try {
    const data = await taskApi.list({ objectType: 'opportunity', objectId: oppId.value, page: 1, size: 50 })
    tasks.value = data.records
  } catch { /* */ }
}

function openCreateTask() {
  taskDialog.visible = true
  Object.assign(taskForm, { title: '', content: '', assigneeId: null, priority: 'NORMAL', dueAt: '' })
}

async function submitTask() {
  if (!taskForm.title) return ElMessage.warning('请输入任务标题')
  try {
    await taskApi.create({ ...taskForm, objectType: 'opportunity', objectId: oppId.value })
    ElMessage.success('任务已创建')
    taskDialog.visible = false
    loadTasks()
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '创建失败')
  }
}

/* ---------- Tab 5: 协作 ---------- */
const collaborators = ref<CollaboratorRow[]>([])
const collabDialog = reactive({ visible: false })
const collabForm = reactive({ userId: null as number | null, permissionJson: '{"view":true,"follow":true}' })

async function loadCollaborators() {
  try { collaborators.value = await oppApi.collaborators(oppId.value) }
  catch { /* */ }
}

async function addCollaborator() {
  if (!collabForm.userId) return ElMessage.warning('请选择用户')
  try {
    await oppApi.addCollaborator(oppId.value, { ...collabForm })
    ElMessage.success('协作人已添加')
    collabDialog.visible = false
    loadCollaborators()
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '添加失败')
  }
}

async function removeCollaborator(cid: number) {
  try {
    await ElMessageBox.confirm('确认移除该协作人？', '确认', { type: 'warning' })
    await oppApi.removeCollaborator(oppId.value, cid)
    ElMessage.success('已移除')
    loadCollaborators()
  } catch { /* */ }
}

/* ---------- Tab 7: 操作日志 ---------- */
const auditLogs = ref<AuditLogRow[]>([])
async function loadAuditLogs() {
  try {
    const data = await auditApi.list({ objectType: 'opportunity', objectId: oppId.value, page: 1, size: 50 })
    auditLogs.value = data.records
  } catch { /* */ }
}

/* ---------- Tab 切换懒加载 ---------- */
function onTabChange(tab: string | number) {
  switch (tab) {
    case 'contacts': loadContacts(); break
    case 'follows': loadFollows(); break
    case 'tasks': loadTasks(); break
    case 'collaborators': loadCollaborators(); break
    case 'logs': loadAuditLogs(); break
  }
}

onMounted(() => { loadBase(); loadUsers() })
</script>

<template>
  <div v-loading="loading">
    <!-- 顶部导航 -->
    <div class="page-head">
      <div>
        <div class="breadcrumb">
          <el-button link type="primary" @click="router.push('/opportunity')">商机管理</el-button>
          <span> / 详情</span>
        </div>
        <h1>{{ detail?.opportunity.title || '加载中...' }}</h1>
      </div>
      <div class="head-actions" v-if="detail">
        <el-button @click="router.push('/opportunity')">返回列表</el-button>
      </div>
    </div>

    <template v-if="detail">
      <!-- 阶段条 -->
      <div class="panel" style="margin-bottom:16px">
        <div class="stage-bar" :style="{ gridTemplateColumns: `repeat(${inProgressStages.length}, 1fr)` }">
          <span v-for="s in inProgressStages" :key="s.id" :class="{ active: s.id === detail.opportunity.stageId }">{{ s.name }}</span>
        </div>
      </div>

      <!-- Tab 面板 -->
      <div class="panel">
        <el-tabs v-model="activeTab" @tab-change="onTabChange">
          <!-- Tab 1: 概况 -->
          <el-tab-pane label="概况" name="overview">
            <div class="info-grid">
              <div><span>客户</span><b>{{ detail.customerName || '-' }}</b></div>
              <div><span>阶段</span><b>{{ detail.stageName || '-' }}</b></div>
              <div><span>金额 / 赢率</span><b>¥{{ Number(detail.opportunity.amount || 0).toLocaleString() }} · {{ detail.opportunity.winRate }}%</b></div>
              <div><span>负责人</span><b>{{ detail.ownerName || '-' }}</b></div>
              <div><span>状态</span>
                <el-tag size="small" :type="statusLabels[detail.opportunity.status]?.type || 'info'">
                  {{ statusLabels[detail.opportunity.status]?.label || detail.opportunity.status }}
                </el-tag>
              </div>
              <div><span>来源</span><b>{{ detail.opportunity.source || '-' }}</b></div>
              <div><span>预计结单</span><b>{{ detail.opportunity.expectedCloseAt?.substring(0, 10) || '-' }}</b></div>
              <div><span>成交金额</span><b>{{ detail.opportunity.dealAmount ? '¥' + Number(detail.opportunity.dealAmount).toLocaleString() : '-' }}</b></div>
            </div>
            <div v-if="detail.opportunity.demand" style="margin-top:16px">
              <h4 style="margin:0 0 8px">需求描述</h4>
              <p style="color:var(--muted);line-height:1.6">{{ detail.opportunity.demand }}</p>
            </div>
            <div v-if="detail.opportunity.status === 'IN_PROGRESS'" class="drawer-actions" style="margin-top:20px">
              <el-select v-perm="'opp:stage:advance'" placeholder="推进到" @change="(v:number) => moveStage(v)" style="width:140px">
                <el-option v-for="s in inProgressStages" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
              <div>
                <el-button v-perm="'opp:win'" type="success" @click="doWin">成交</el-button>
                <el-button v-perm="'opp:lose'" type="danger" @click="doLose">输单</el-button>
              </div>
            </div>
          </el-tab-pane>

          <!-- Tab 2: 联系人 -->
          <el-tab-pane label="联系人" name="contacts">
            <el-table :data="contacts" stripe>
              <el-table-column prop="name" label="姓名" width="120" />
              <el-table-column prop="title" label="职务" width="120" />
              <el-table-column prop="mobile" label="手机" width="140" />
              <el-table-column prop="email" label="邮箱" min-width="180" />
              <el-table-column label="关键人" width="80">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.isKeyPerson ? 'danger' : 'info'">{{ row.isKeyPerson ? '是' : '否' }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!contacts.length" description="暂无联系人" />
          </el-tab-pane>

          <!-- Tab 3: 跟进 -->
          <el-tab-pane :label="`跟进（${follows.length}）`" name="follows">
            <!-- 新增跟进 -->
            <div style="margin-bottom:20px;padding:16px;background:var(--bg);border-radius:12px">
              <el-form :inline="true">
                <el-select v-model="followForm.followType" style="width:100px">
                  <el-option label="电话" value="电话" />
                  <el-option label="拜访" value="拜访" />
                  <el-option label="邮件" value="邮件" />
                  <el-option label="微信" value="微信" />
                  <el-option label="方案" value="方案" />
                </el-select>
                <el-input v-model="followForm.content" placeholder="跟进内容" style="width:280px" />
                <el-input v-model="followForm.result" placeholder="结果" style="width:160px" />
                <el-date-picker v-model="followForm.nextTime" type="date" value-format="YYYY-MM-DD" placeholder="下次跟进" style="width:140px" />
                <el-button v-perm="'opp:follow:create'" type="primary" @click="addFollow">添加</el-button>
              </el-form>
            </div>
            <!-- 跟进列表 -->
            <div class="timeline">
              <div v-for="f in follows" :key="f.id" class="timeline-item">
                <div class="timeline-dot"></div>
                <div class="timeline-body">
                  <div style="display:flex;justify-content:space-between;align-items:center">
                    <el-tag size="small">{{ f.followType }}</el-tag>
                    <span style="font-size:12px;color:var(--muted)">{{ f.createdAt }}</span>
                  </div>
                  <p v-if="f.content" style="margin:6px 0 0;color:var(--text)">{{ f.content }}</p>
                  <p v-if="f.result" style="margin:4px 0 0;font-size:13px;color:var(--muted)">结果：{{ f.result }}</p>
                  <p v-if="f.nextTime" style="margin:4px 0 0;font-size:13px;color:var(--blue)">下次跟进：{{ f.nextTime?.substring(0, 10) }}</p>
                </div>
              </div>
            </div>
            <el-empty v-if="!follows.length" description="暂无跟进记录" />
          </el-tab-pane>

          <!-- Tab 4: 任务 -->
          <el-tab-pane :label="`任务（${tasks.length}）`" name="tasks">
            <div style="margin-bottom:12px">
              <el-button v-perm="'task:create'" type="primary" size="small" @click="openCreateTask">新建任务</el-button>
            </div>
            <el-table :data="tasks" stripe>
              <el-table-column prop="title" label="任务" min-width="180" />
              <el-table-column label="负责人" width="100">
                <template #default="{ row }">{{ userMap[row.assigneeId!] || '-' }}</template>
              </el-table-column>
              <el-table-column label="优先级" width="80">
                <template #default="{ row }">
                  <el-tag size="small" :type="(row.priority === 'HIGH' ? 'danger' : row.priority === 'LOW' ? 'info' : 'primary') as any">
                    {{ row.priority === 'HIGH' ? '高' : row.priority === 'LOW' ? '低' : '中' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="(row.status === 'DONE' ? 'success' : row.status === 'CANCELLED' ? 'info' : row.status === 'OVERDUE' ? 'danger' : 'primary') as any">
                    {{ row.status === 'PENDING' ? '待办' : row.status === 'DOING' ? '进行中' : row.status === 'DONE' ? '完成' : row.status === 'OVERDUE' ? '逾期' : '已取消' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="截止" width="110">
                <template #default="{ row }">{{ row.dueAt?.substring(0, 10) || '-' }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!tasks.length" description="暂无关联任务" />

            <!-- 新建任务对话框 -->
            <el-dialog v-model="taskDialog.visible" title="新建任务" width="480px">
              <el-form label-width="80px">
                <el-form-item label="标题" required><el-input v-model="taskForm.title" /></el-form-item>
                <el-form-item label="描述"><el-input v-model="taskForm.content" type="textarea" :rows="2" /></el-form-item>
                <el-form-item label="指派给">
                  <el-select v-model="taskForm.assigneeId" clearable filterable style="width:100%">
                    <el-option v-for="u in users" :key="u.id" :label="u.realName || u.username" :value="u.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="优先级">
                  <el-select v-model="taskForm.priority" style="width:100%">
                    <el-option label="高" value="HIGH" />
                    <el-option label="中" value="NORMAL" />
                    <el-option label="低" value="LOW" />
                  </el-select>
                </el-form-item>
                <el-form-item label="截止日期">
                  <el-date-picker v-model="taskForm.dueAt" type="date" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-form>
              <template #footer>
                <el-button @click="taskDialog.visible = false">取消</el-button>
                <el-button type="primary" @click="submitTask">创建</el-button>
              </template>
            </el-dialog>
          </el-tab-pane>

          <!-- Tab 5: 协作 -->
          <el-tab-pane label="协作" name="collaborators">
            <div style="margin-bottom:12px">
              <el-button v-perm="'opp:collab:add'" type="primary" size="small" @click="collabDialog.visible = true">添加协作人</el-button>
            </div>
            <el-table :data="collaborators" stripe>
              <el-table-column label="用户" width="140">
                <template #default="{ row }">{{ userMap[row.userId] || row.userId }}</template>
              </el-table-column>
              <el-table-column prop="permissionJson" label="权限" min-width="200">
                <template #default="{ row }">
                  <template v-if="row.permissionJson">
                    <el-tag v-for="(v, k) in JSON.parse(row.permissionJson)" :key="k" size="small"
                      :type="v ? 'success' : 'info'" style="margin:2px">{{ k }}</el-tag>
                  </template>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-button v-perm="'opp:collab:remove'" link type="danger" size="small" @click="removeCollaborator(row.id)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!collaborators.length" description="暂无协作人" />

            <!-- 添加协作人对话框 -->
            <el-dialog v-model="collabDialog.visible" title="添加协作人" width="440px">
              <el-form label-width="80px">
                <el-form-item label="用户" required>
                  <el-select v-model="collabForm.userId" filterable style="width:100%">
                    <el-option v-for="u in users" :key="u.id" :label="u.realName || u.username" :value="u.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="权限">
                  <el-input v-model="collabForm.permissionJson" type="textarea" :rows="2" placeholder='{"view":true,"follow":true}' />
                </el-form-item>
              </el-form>
              <template #footer>
                <el-button @click="collabDialog.visible = false">取消</el-button>
                <el-button type="primary" @click="addCollaborator">添加</el-button>
              </template>
            </el-dialog>
          </el-tab-pane>

          <!-- Tab 6: 报价（V1.1 占位） -->
          <el-tab-pane label="报价" name="quote">
            <el-empty description="报价功能将于 V1.1 上线，敬请期待" />
          </el-tab-pane>

          <!-- Tab 7: 订单（V1.1 占位） -->
          <el-tab-pane label="订单" name="order">
            <el-empty description="订单功能将于 V1.1 上线，敬请期待" />
          </el-tab-pane>

          <!-- Tab 8: 附件 -->
          <el-tab-pane label="附件" name="attachments">
            <AttachmentPanel v-if="activeTab === 'attachments'" object-type="opportunity" :object-id="oppId" />
          </el-tab-pane>

          <!-- Tab 9: 操作日志 -->
          <el-tab-pane label="操作日志" name="logs">
            <el-table :data="auditLogs" stripe>
              <el-table-column prop="userName" label="操作人" width="100" />
              <el-table-column prop="action" label="操作" min-width="180" />
              <el-table-column label="结果" width="80">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.result === 'SUCCESS' ? 'success' : 'danger'">{{ row.result }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="ip" label="IP" width="130" />
              <el-table-column prop="createdAt" label="时间" width="170" />
            </el-table>
            <el-empty v-if="!auditLogs.length" description="暂无操作记录" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </template>
  </div>
</template>

<style scoped>
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.info-grid > div { display: flex; flex-direction: column; gap: 4px; }
.info-grid span { font-size: 12px; color: var(--muted); }
.info-grid b { font-size: 14px; color: var(--text); }
.stage-bar {
  display: grid; gap: 0; text-align: center; font-size: 13px;
}
.stage-bar span {
  padding: 8px 12px; background: var(--bg); border-radius: 8px;
  transition: all .2s;
}
.stage-bar span.active {
  background: var(--blue); color: #fff; font-weight: 600;
}
.drawer-actions {
  display: flex; gap: 12px; align-items: center; flex-wrap: wrap;
}
.timeline { display: flex; flex-direction: column; gap: 16px; padding-left: 16px; border-left: 2px solid var(--line); }
.timeline-item { position: relative; padding-left: 16px; }
.timeline-dot {
  position: absolute; left: -22px; top: 6px; width: 10px; height: 10px;
  border-radius: 50%; background: var(--blue);
}
</style>
