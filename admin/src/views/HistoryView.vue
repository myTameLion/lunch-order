<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { DataAnalysis, Download, Food, Dish } from '@element-plus/icons-vue'
import { exportSummary, getSummary } from '@/api'
import type { RangeSummary } from '@/api/types'
import BaseChart from '@/components/BaseChart.vue'
import StatCard from '@/components/StatCard.vue'
import {
  COLOR_MAIN,
  COLOR_NON_SPICY,
  COLOR_SPICY,
  buildDailyBarOption,
  buildTopBarOption,
  participationRate,
  sortUsersByDays,
  toDailySeries,
} from '@/utils/chartData'
import { daysBetweenInclusive, lastNDays } from '@/utils/date'
import { downloadBlob, fallbackExportName, parseFilenameFromDisposition } from '@/utils/download'

const loading = ref(false)
const exporting = ref(false)
const range = ref<[string, string]>(lastNDays(7))
const summary = ref<RangeSummary | null>(null)

async function load() {
  const [from, to] = range.value
  if (!from || !to) return
  loading.value = true
  try {
    summary.value = await getSummary(from, to)
  } finally {
    loading.value = false
  }
}
onMounted(load)
watch(range, load)

const dailyOption = computed(() => {
  const series = toDailySeries(summary.value?.daily ?? [])
  return buildDailyBarOption(series.dates, series.spicy, series.nonSpicy)
})
const topOption = computed(() => buildTopBarOption(summary.value?.perUser ?? [], 10))
const sortedUsers = computed(() => sortUsersByDays(summary.value?.perUser ?? []))
const rangeDays = computed(() => daysBetweenInclusive(range.value[0], range.value[1]))

async function doExport() {
  const [from, to] = range.value
  exporting.value = true
  try {
    const resp = await exportSummary(from, to)
    const name = parseFilenameFromDisposition(resp.headers?.['content-disposition']) ?? fallbackExportName(from, to)
    downloadBlob(resp.data, name)
    ElMessage.success('已导出 Excel')
  } finally {
    exporting.value = false
  }
}
</script>

<template>
  <div v-loading="loading">
    <el-card shadow="never" class="panel toolbar">
      <div class="toolbar-inner">
        <el-date-picker
          v-model="range"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          :clearable="false"
        />
        <el-button type="primary" :icon="Download" :loading="exporting" @click="doExport">导出 Excel</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" class="stats">
      <el-col :span="8">
        <StatCard label="区间点餐总数" :value="summary?.totalOrders ?? 0" :icon="DataAnalysis" :color="COLOR_MAIN" />
      </el-col>
      <el-col :span="8">
        <StatCard label="要辣合计" :value="summary?.totalSpicy ?? 0" :icon="Food" :color="COLOR_SPICY" />
      </el-col>
      <el-col :span="8">
        <StatCard label="不要辣合计" :value="summary?.totalNonSpicy ?? 0" :icon="Dish" :color="COLOR_NON_SPICY" />
      </el-col>
    </el-row>

    <el-row :gutter="16" class="charts">
      <el-col :span="14">
        <el-card shadow="never" class="panel">
          <template #header><b>每日点餐人数（要辣 / 不要辣堆叠）</b></template>
          <BaseChart :option="dailyOption" height="320px" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" class="panel">
          <template #header><b>点餐天数 TOP10</b></template>
          <BaseChart :option="topOption" height="320px" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="panel">
      <template #header><b>人员汇总（{{ range[0] }} 至 {{ range[1] }}，共 {{ rangeDays }} 天）</b></template>
      <el-table :data="sortedUsers" stripe>
        <el-table-column prop="displayName" label="姓名" min-width="120" />
        <el-table-column prop="loginName" label="登录名" min-width="120" />
        <el-table-column prop="days" label="点餐天数" width="100" sortable />
        <el-table-column prop="spicyDays" label="要辣天数" width="100" />
        <el-table-column prop="nonSpicyDays" label="不要辣天数" width="110" />
        <el-table-column label="参与率" min-width="180">
          <template #default="{ row }">
            <el-progress
              :percentage="participationRate(row.days, rangeDays)"
              :stroke-width="10"
              :color="COLOR_MAIN"
            />
          </template>
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
.charts {
  margin-bottom: 16px;
}
</style>
