<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Star } from '@element-plus/icons-vue'
import { getEvaluations } from '@/api'
import type { EvaluationsResponse } from '@/api/types'
import StatCard from '@/components/StatCard.vue'
import { COLOR_MAIN, COLOR_SPICY } from '@/utils/chartData'
import { formatDateTime } from '@/utils/date'

const loading = ref(false)
const date = ref(today())
const data = ref<EvaluationsResponse | null>(null)

function today(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function load() {
  loading.value = true
  try {
    data.value = await getEvaluations(date.value)
  } finally {
    loading.value = false
  }
}
onMounted(load)
watch(date, load)

const avgText = computed(() => (data.value ? data.value.avgRating.toFixed(1) : '0.0'))
</script>

<template>
  <div v-loading="loading">
    <el-card shadow="never" class="panel toolbar">
      <div class="toolbar-inner">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" :clearable="false" />
        <span class="hint">每人每天仅保留最后一次评价</span>
      </div>
    </el-card>

    <el-row :gutter="16" class="stats">
      <el-col :span="12">
        <StatCard label="评价人数" :value="data?.count ?? 0" :icon="ChatDotRound" :color="COLOR_MAIN" />
      </el-col>
      <el-col :span="12">
        <StatCard label="平均评分" :value="`${avgText} / 5.0`" :icon="Star" :color="COLOR_SPICY" />
      </el-col>
    </el-row>

    <el-card shadow="never" class="panel">
      <template #header><b>{{ date }} 的点餐评价（{{ data?.evaluations.length ?? 0 }} 人）</b></template>
      <el-table :data="data?.evaluations ?? []" stripe empty-text="这一天还没有人评价">
        <el-table-column prop="displayName" label="姓名" min-width="110" />
        <el-table-column prop="loginName" label="登录名" min-width="110" />
        <el-table-column label="评分" width="180">
          <template #default="{ row }">
            <el-rate :model-value="row.rating" disabled />
          </template>
        </el-table-column>
        <el-table-column label="评语" min-width="200">
          <template #default="{ row }">{{ row.comment || '—' }}</template>
        </el-table-column>
        <el-table-column label="评价时间" min-width="150">
          <template #default="{ row }">{{ formatDateTime(row.ratedAt) }}</template>
        </el-table-column>
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
