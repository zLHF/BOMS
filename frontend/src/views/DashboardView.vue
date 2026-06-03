<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

// 工作台空壳 KPI（阶段 0 占位，V1.0 接真实统计接口）
const kpis = [
  { label: '我的商机', value: '—', icon: '📈', cls: 'blue' },
  { label: '本月新增', value: '—', icon: '✨', cls: 'green' },
  { label: '待跟进', value: '—', icon: '⏰', cls: 'orange' },
  { label: '成交金额', value: '—', icon: '💰', cls: 'purple' },
]
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="breadcrumb">业务 / 工作台</div>
        <h1>工作台</h1>
        <p>欢迎，{{ auth.realName || auth.username }}。当前为阶段 0 骨架，后端 M0 链路已贯通。</p>
      </div>
    </div>

    <div class="kpi-grid">
      <div class="kpi-card" v-for="k in kpis" :key="k.label">
        <div class="kpi-icon" :class="k.cls">{{ k.icon }}</div>
        <div>
          <span>{{ k.label }}</span>
          <b>{{ k.value }}</b>
        </div>
      </div>
    </div>

    <div class="panel">
      <h3>已授权的菜单/操作权限码（共 {{ auth.permissions.length }} 项）</h3>
      <p style="color:var(--muted);margin:0 0 12px;">
        以下权限码来自后端 token → 角色 → 角色权限映射，驱动左侧菜单与按钮显隐（v-perm）。
      </p>
      <div style="display:flex;flex-wrap:wrap;gap:8px;">
        <span class="stage blue" v-for="p in auth.permissions" :key="p">{{ p }}</span>
      </div>
    </div>
  </div>
</template>
