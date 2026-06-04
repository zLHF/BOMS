<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { followsApi } from '@/services/business'
import type { FollowListRow, Page } from '@/types'

const router = useRouter()

const loading = ref(false)
const rows = ref<FollowListRow[]>([])
const total = ref(0)
const viewMode = ref<'timeline' | 'list'>('timeline')

const filter = reactive({
  keyword: '',
  followType: '',
  start: '',
  end: '',
  page: 1,
  size: 20,
})

const followTypes = ['电话', '拜访', '会议', '微信', '邮件', '方案沟通']

async function load() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: filter.page,
      size: filter.size,
    }
    if (filter.keyword) params.keyword = filter.keyword
    if (filter.followType) params.followType = filter.followType
    if (filter.start) params.start = filter.start
    if (filter.end) params.end = filter.end

    const data: Page<FollowListRow> = await followsApi.list(params)
    rows.value = data.records
    total.value = data.total
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onPageChange(page: number) {
  filter.page = page
  load()
}

function goDetail(oppId: number) {
  router.push(`/opportunity/${oppId}`)
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="breadcrumb">业务 / 跟进记录</div>
        <h1>跟进记录</h1>
        <p>查看所有商机的跟进历史，支持按方式和时间筛选。</p>
      </div>
    </div>

    <div class="panel">
      <!-- 筛选区 -->
      <div class="filter-bar">
        <el-input v-model="filter.keyword" placeholder="搜索内容" clearable style="width:200px" @clear="load" @keyup.enter="load" />
        <el-select v-model="filter.followType" placeholder="跟进方式" clearable style="width:130px" @change="load">
          <el-option v-for="t in followTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-date-picker v-model="filter.start" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="width:140px" @change="load" />
        <span style="color:var(--muted)">~</span>
        <el-date-picker v-model="filter.end" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" style="width:140px" @change="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <div style="flex:1" />
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="timeline">时间线</el-radio-button>
          <el-radio-button value="list">列表</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 时间线视图 -->
      <div v-if="viewMode === 'timeline'" v-loading="loading" class="timeline" style="margin-top:16px">
        <div v-for="f in rows" :key="f.id" class="timeline-item">
          <div class="timeline-dot"></div>
          <div class="timeline-body">
            <div class="follow-header">
              <el-tag size="small">{{ f.followType || '-' }}</el-tag>
              <el-button link type="primary" @click="goDetail(f.opportunityId)">{{ f.opportunityTitle }}</el-button>
              <span class="follow-meta">{{ f.creatorName }} · {{ f.createdAt?.substring(0, 16).replace('T', ' ') }}</span>
            </div>
            <p v-if="f.content" class="follow-content">{{ f.content }}</p>
            <p v-if="f.result" class="follow-result">结果：{{ f.result }}</p>
            <p v-if="f.nextTime" class="follow-next">下次跟进：{{ f.nextTime?.substring(0, 10) }}</p>
          </div>
        </div>
        <el-empty v-if="!loading && !rows.length" description="暂无跟进记录" />
      </div>

      <!-- 列表视图 -->
      <el-table v-else :data="rows" v-loading="loading" stripe style="margin-top:16px">
        <el-table-column label="商机" min-width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.opportunityId)">{{ row.opportunityTitle }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="方式" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.followType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="跟进内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="result" label="结果" width="140" show-overflow-tooltip />
        <el-table-column prop="creatorName" label="跟进人" width="100" />
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ row.createdAt?.substring(0, 16).replace('T', ' ') }}</template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-if="total > filter.size"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="filter.size"
        :current-page="filter.page"
        @current-change="onPageChange"
        style="margin-top:16px;justify-content:center"
      />
    </div>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.timeline {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-left: 16px;
  border-left: 2px solid var(--line);
}
.timeline-item {
  position: relative;
  padding-left: 16px;
}
.timeline-dot {
  position: absolute;
  left: -22px;
  top: 6px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--blue);
}
.follow-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.follow-meta {
  font-size: 12px;
  color: var(--muted);
}
.follow-content {
  margin: 6px 0 0;
  color: var(--text);
  line-height: 1.5;
}
.follow-result {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--muted);
}
.follow-next {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--blue);
}
</style>
