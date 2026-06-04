<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { poolConfigApi } from '@/services/business'
import type { PoolConfigRow } from '@/types'

const loading = ref(false)
const config = reactive<PoolConfigRow>({
  id: 0,
  autoRecycleEnabled: 1,
  noFollowDays: 30,
  protectionDays: 7,
  personalLimit: 50,
})

async function loadConfig() {
  loading.value = true
  try {
    const data = await poolConfigApi.get()
    Object.assign(config, data)
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '加载配置失败')
  } finally {
    loading.value = false
  }
}

async function saveConfig() {
  try {
    await poolConfigApi.update({
      autoRecycleEnabled: config.autoRecycleEnabled,
      noFollowDays: config.noFollowDays,
      protectionDays: config.protectionDays,
      personalLimit: config.personalLimit,
    })
    ElMessage.success('配置已保存')
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '保存失败')
  }
}

async function executeRecycle() {
  try {
    await ElMessageBox.confirm(
      '确认立即执行一次自动回收？系统将扫描所有符合回收条件的商机和客户并回收到公海池。',
      '手动触发回收',
      { type: 'warning', confirmButtonText: '执行', cancelButtonText: '取消' },
    )
    const result = await poolConfigApi.execute()
    ElMessage.success(result || '回收任务已执行')
    loadConfig()
  } catch { /* user cancelled */ }
}

onMounted(loadConfig)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="breadcrumb">设置 / 公海配置</div>
        <h1>公海池配置</h1>
        <p>配置公海池自动回收规则。超过设定天数无有效跟进的商机和客户将自动回收到公海池。</p>
      </div>
    </div>

    <div class="panel" v-loading="loading">
      <el-form label-width="140px" style="max-width:560px">
        <el-form-item label="自动回收">
          <el-switch
            v-model="config.autoRecycleEnabled"
            :active-value="1"
            :inactive-value="0"
            active-text="已启用"
            inactive-text="已关闭"
          />
        </el-form-item>

        <el-form-item label="无跟进天数阈值">
          <el-input-number v-model="config.noFollowDays" :min="7" :max="365" :step="1" />
          <span style="margin-left:8px;color:var(--muted);font-size:13px">天（超过此天数无有效跟进将自动回收）</span>
        </el-form-item>

        <el-form-item label="领取保护期">
          <el-input-number v-model="config.protectionDays" :min="1" :max="90" :step="1" />
          <span style="margin-left:8px;color:var(--muted);font-size:13px">天（领取后此期间内不自动回收）</span>
        </el-form-item>

        <el-form-item label="个人领取上限">
          <el-input-number v-model="config.personalLimit" :min="1" :max="500" :step="1" />
          <span style="margin-left:8px;color:var(--muted);font-size:13px">个（每人最多领取数量）</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="saveConfig">保存配置</el-button>
          <el-button @click="executeRecycle">手动触发回收</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>
