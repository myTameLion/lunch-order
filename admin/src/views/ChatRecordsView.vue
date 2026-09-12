<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ChatLineRound } from '@element-plus/icons-vue'
import { getChatByDate } from '@/api'
import type { ChatResponse } from '@/api/types'
import StatCard from '@/components/StatCard.vue'
import { COLOR_MAIN } from '@/utils/chartData'
import { formatDateTime } from '@/utils/date'

const loading = ref(false)
const date = ref(today())
const data = ref<ChatResponse | null>(null)

function today(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function load() {
  loading.value = true
  try {
    data.value = await getChatByDate(date.value)
  } finally {
    loading.value = false
  }
}
onMounted(load)
watch(date, load)
</script>

<template>
  <div v-loading="loading">
    <el-card shadow="never" class="panel toolbar">
      <div class="toolbar-inner">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" :clearable="false" />
        <span class="hint">公共聊天频道 · 可查看任意历史日期（客户端仅展示当天）</span>
      </div>
    </el-card>

    <el-row :gutter="16" class="stats">
      <el-col :span="24">
        <StatCard label="当日消息数" :value="data?.count ?? 0" :icon="ChatLineRound" :color="COLOR_MAIN" />
      </el-col>
    </el-row>

    <el-card shadow="never" class="panel">
      <template #header><b>{{ date }} 的聊天记录（{{ data?.messages.length ?? 0 }} 条）</b></template>
      <el-table :data="data?.messages ?? []" stripe empty-text="这一天没有聊天消息">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.sentAt) }}</template>
        </el-table-column>
        <el-table-column prop="displayName" label="姓名" min-width="110" />
        <el-table-column prop="loginName" label="登录名" min-width="110" />
        <el-table-column prop="content" label="消息内容" min-width="300" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.panel {
  border-radius: 10px;
}
.toolbar-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.stats {
  margin: 16px 0;
}
.hint {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
