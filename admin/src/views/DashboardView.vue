<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Dish, Food, UserFilled } from '@element-plus/icons-vue'
import { getTodayAll } from '@/api'
import type { TodayAllResponse } from '@/api/types'
import BaseChart from '@/components/BaseChart.vue'
import StatCard from '@/components/StatCard.vue'
import {
  COLOR_MAIN,
  COLOR_NON_SPICY,
  COLOR_SPICY,
  bucketByTimeSlot,
  buildDonutOption,
  buildTrendOption,
  spicyDistribution,
  timeSlotLabels,
} from '@/utils/chartData'
import { formatDateTime } from '@/utils/date'

const loading = ref(false)
const data = ref<TodayAllResponse | null>(null)
const spicyFilter = ref<'all' | 'spicy' | 'non'>('all')

async function load() {
  loading.value = true
  try {
    data.value = await getTodayAll()
  } finally {
    loading.value = false
  }
}
// 30 秒轮询保持"实时"（D-008）
let pollTimer: number | undefined
onMounted(() => {
  void load()
  pollTimer = window.setInterval(load, 30_000)
})
onBeforeUnmount(() => {
  if (pollTimer) window.clearInterval(pollTimer)
})

const donutOption = computed(() => buildDonutOption(spicyDistribution(data.value?.orders ?? [])))
const trendOption = computed(() =>
  buildTrendOption(timeSlotLabels(), bucketByTimeSlot(data.value?.orders ?? [])),
)

const filteredOrders = computed(() => {
  const orders = data.value?.orders ?? []
  if (spicyFilter.value === 'spicy') return orders.filter((o) => o.spicy)
  if (spicyFilter.value === 'non') return orders.filter((o) => !o.spicy)
  return orders
})
</script>

<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col :span="8">
        <StatCard label="今日点餐人数" :value="data?.total ?? 0" :icon="UserFilled" :color="COLOR_MAIN" />
      </el-col>
      <el-col :span="8">
        <StatCard label="要辣（份）" :value="data?.spicy ?? 0" :icon="Food" :color="COLOR_SPICY" />
      </el-col>
      <el-col :span="8">
        <StatCard label="不要辣（份）" :value="data?.nonSpicy ?? 0" :icon="Dish" :color="COLOR_NON_SPICY" />
      </el-col>
    </el-row>

    <el-row :gutter="16" class="charts">
      <el-col :span="9">
        <el-card shadow="never" class="panel">
          <template #header><b>辣度分布</b></template>
          <BaseChart :option="donutOption" height="300px" />
        </el-card>
      </el-col>
      <el-col :span="15">
        <el-card shadow="never" class="panel">
          <template #header><b>分时段登记趋势（14:00 - 18:00）</b></template>
          <BaseChart :option="trendOption" height="300px" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="panel">
      <template #header>
        <div class="table-head">
          <b>今日点餐名单（{{ filteredOrders.length }} 人）</b>
          <el-radio-group v-model="spicyFilter" size="small">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="spicy">要辣</el-radio-button>
            <el-radio-button value="non">不要辣</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="filteredOrders" stripe empty-text="今天还没有人点餐">
        <el-table-column prop="displayName" label="姓名" min-width="120" />
        <el-table-column prop="loginName" label="登录名" min-width="120" />
        <el-table-column label="辣度" width="110">
          <template #default="{ row }">
            <el-tag :type="row.spicy ? 'danger' : 'success'" effect="light">
              {{ row.spicy ? '🌶️ 要辣' : '不要辣' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="登记时间" min-width="150">
          <template #default="{ row }">{{ formatDateTime(row.orderedAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.charts {
  margin: 16px 0;
}
.panel {
  border-radius: 10px;
}
.table-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
