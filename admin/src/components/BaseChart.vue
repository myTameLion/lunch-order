<script setup lang="ts">
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{
  /** ECharts option（由 chartData.ts 纯函数构建） */
  option: Record<string, unknown>
  /** 容器高度 */
  height?: string
}>()

const container = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null

function render() {
  if (!container.value) return
  if (!chart) {
    chart = echarts.init(container.value)
  }
  chart.setOption(props.option, true)
}

function handleResize() {
  chart?.resize()
}

onMounted(() => {
  render()
  // 监听窗口尺寸变化，保证图表自适应
  window.addEventListener('resize', handleResize)
})

watch(
  () => props.option,
  () => render(),
  { deep: true },
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div ref="container" class="base-chart" :style="{ height: height ?? '320px', width: '100%' }"></div>
</template>
