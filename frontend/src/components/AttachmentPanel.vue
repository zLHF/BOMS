<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, Download, Delete } from '@element-plus/icons-vue'
import { fileApi } from '@/services/file'
import type { AttachmentRow, SignUploadResp } from '@/types'

const props = defineProps<{
  objectType: string
  objectId: number
  /** 是否显示删除按钮，默认 true */
  deletable?: boolean
}>()

const files = ref<AttachmentRow[]>([])
const uploading = ref(false)
const uploadProgress = ref(0)

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function loadFiles() {
  try {
    files.value = await fileApi.list(props.objectType, props.objectId)
  } catch { /* */ }
}

/** 选择文件后触发上传流程 */
async function handleUpload(rawFile: File) {
  if (rawFile.size > 50 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过 50MB')
    return
  }

  uploading.value = true
  uploadProgress.value = 0
  try {
    // 1. 获取预签名 URL
    const resp: SignUploadResp = await fileApi.signUpload({
      fileName: rawFile.name,
      contentType: rawFile.type,
      objectType: props.objectType,
      objectId: props.objectId,
      fileSize: rawFile.size,
    })

    uploadProgress.value = 30

    // 2. 直传 MinIO
    await fetch(resp.uploadUrl, {
      method: 'PUT',
      body: rawFile,
      headers: { 'Content-Type': rawFile.type },
    })

    uploadProgress.value = 80

    // 3. 确认上传
    await fileApi.confirm(resp.attachmentId)

    uploadProgress.value = 100
    ElMessage.success('上传成功')
    loadFiles()
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '上传失败')
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}

function onFileChange(uploadFile: any) {
  if (uploadFile?.raw) {
    handleUpload(uploadFile.raw)
  }
}

async function downloadFile(file: AttachmentRow) {
  try {
    const url = await fileApi.signDownload(file.id)
    window.open(url, '_blank')
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '下载失败')
  }
}

async function deleteFile(file: AttachmentRow) {
  try {
    await ElMessageBox.confirm(`确认删除文件「${file.fileName}」？`, '确认删除', { type: 'warning' })
    await fileApi.remove(file.id)
    ElMessage.success('已删除')
    loadFiles()
  } catch { /* user cancelled */ }
}

onMounted(loadFiles)
</script>

<template>
  <div class="attachment-panel">
    <!-- 上传区域 -->
    <div class="upload-area">
      <el-upload
        :auto-upload="false"
        :show-file-list="false"
        :on-change="onFileChange"
        :disabled="uploading"
        multiple
      >
        <el-button type="primary" :icon="UploadFilled" :loading="uploading">
          {{ uploading ? '上传中...' : '上传文件' }}
        </el-button>
      </el-upload>
      <el-progress v-if="uploading" :percentage="uploadProgress" :stroke-width="4" style="width:200px;margin-left:16px" />
      <span class="upload-tip">支持图片、PDF、Word、Excel、压缩包等，单文件不超过 50MB</span>
    </div>

    <!-- 文件列表 -->
    <el-table v-if="files.length" :data="files" stripe style="margin-top:12px">
      <el-table-column prop="fileName" label="文件名" min-width="240" show-overflow-tooltip />
      <el-table-column label="大小" width="100">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="类型" width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ row.contentType || '-' }}</template>
      </el-table-column>
      <el-table-column label="上传时间" width="170">
        <template #default="{ row }">{{ row.createdAt?.substring(0, 16).replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Download" @click="downloadFile(row as AttachmentRow)">下载</el-button>
          <el-button v-if="deletable !== false" link type="danger" :icon="Delete" @click="deleteFile(row as AttachmentRow)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else description="暂无附件" />
  </div>
</template>

<style scoped>
.upload-area {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.upload-tip {
  font-size: 12px;
  color: var(--muted);
}
</style>
