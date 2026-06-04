<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { oppApi } from '@/services/business'
import { getToken } from '@/services/request'
import type { ImportProgressResp } from '@/types'

const emit = defineEmits<{ done: [] }>()

const visible = ref(false)
const step = ref(0) // 0=上传 1=导入中 2=结果
const uploading = ref(false)
const polling = ref(false)
const taskId = ref<number | null>(null)
const progress = reactive<ImportProgressResp>({ taskId: 0, status: '', total: 0, success: 0, failed: 0 })
const fileName = ref('')

function open() {
  step.value = 0
  taskId.value = null
  fileName.value = ''
  Object.assign(progress, { taskId: 0, status: '', total: 0, success: 0, failed: 0 })
  visible.value = true
}

function downloadTemplate() {
  const token = getToken()
  if (!token) {
    ElMessage.warning('请先登录后再下载模板')
    return
  }

  fetch('/api/opportunities/import/template', {
    headers: { Authorization: `Bearer ${token}` },
  }).then(async r => {
    if (!r.ok) throw new Error(await r.text())
    return r.blob()
  }).then(blob => {
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    link.href = url
    link.download = '商机导入模板.xlsx'
    link.click()
    URL.revokeObjectURL(url)
  }).catch(() => ElMessage.error('下载模板失败'))
}

async function handleUpload(uploadFile: any) {
  if (!uploadFile?.raw) return
  const file = uploadFile.raw as File
  if (!file.name.endsWith('.xlsx') && !file.name.endsWith('.xls')) {
    ElMessage.warning('请上传 Excel 文件（.xlsx / .xls）')
    return
  }

  fileName.value = file.name
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await oppApi.startImport(fd)
    taskId.value = res.taskId
    step.value = 1
    pollProgress(res.taskId)
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function pollProgress(id: number) {
  polling.value = true
  while (polling.value) {
    try {
      const p = await oppApi.importProgress(id)
      Object.assign(progress, p)
      if (p.status === 'DONE' || p.status === 'FAILED') {
        polling.value = false
        step.value = 2
        if (p.status === 'DONE' && p.failed === 0) {
          ElMessage.success(`导入成功：${p.success} 条`)
          emit('done')
        }
        break
      }
    } catch {
      polling.value = false
      break
    }
    await new Promise(r => setTimeout(r, 1500))
  }
}

function close() {
  polling.value = false
  visible.value = false
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" title="批量导入商机" width="560px" @close="close">
    <!-- Step 0: 上传 -->
    <div v-if="step === 0">
      <div style="margin-bottom:12px">
        <el-button size="small" @click="downloadTemplate">下载导入模板</el-button>
      </div>
      <el-upload
        :auto-upload="false"
        :show-file-list="false"
        :on-change="handleUpload"
        :disabled="uploading"
        drag
      >
        <el-icon size="40" style="color:var(--muted)"><UploadFilled /></el-icon>
        <p>将 Excel 文件拖到此处，或 <em>点击上传</em></p>
        <template #tip>
          <p style="font-size:12px;color:var(--muted)">仅支持 .xlsx / .xls，单次最多 10,000 行</p>
        </template>
      </el-upload>
      <div v-if="uploading" style="margin-top:12px">
        <el-progress :percentage="30" :stroke-width="4" status="" :indeterminate="true" />
        <p style="font-size:13px;color:var(--muted)">正在上传 {{ fileName }}...</p>
      </div>
    </div>

    <!-- Step 1: 导入中 -->
    <div v-if="step === 1" style="text-align:center;padding:24px 0">
      <el-progress :percentage="Math.round((progress.success + progress.failed) / Math.max(progress.total, 1) * 100)" :stroke-width="8" />
      <p style="margin-top:12px;color:var(--text)">正在导入中...</p>
      <p style="color:var(--muted);font-size:13px">已处理 {{ progress.success + progress.failed }} / {{ progress.total || '...' }} 行</p>
    </div>

    <!-- Step 2: 结果 -->
    <div v-if="step === 2">
      <el-result
        :icon="progress.failed === 0 ? 'success' : 'warning'"
        :title="progress.failed === 0 ? '导入完成' : '导入完成（部分失败）'"
      >
        <template #sub-title>
          <div style="font-size:14px">
            <p>总行数：<b>{{ progress.total }}</b></p>
            <p style="color:#67c23a">成功：<b>{{ progress.success }}</b> 条</p>
            <p v-if="progress.failed" style="color:#f56c6c">失败：<b>{{ progress.failed }}</b> 条</p>
          </div>
        </template>
        <template #extra>
          <el-button type="primary" @click="close">完成</el-button>
        </template>
      </el-result>
    </div>
  </el-dialog>
</template>
