<script setup lang="ts">
import { useRoute } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()
const title = computed(() => (route.meta.title as string) || '模块')
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="breadcrumb">{{ route.meta.group }} / {{ title }}</div>
        <h1>{{ title }}</h1>
        <p>阶段 0 占位页。该模块将在 V1.0 对应 Sprint 落地（页面区块见 06-页面与交互矩阵）。</p>
      </div>
    </div>
    <div class="panel">
      <el-empty :description="`「${title}」开发中`" />
    </div>
  </div>
</template>
