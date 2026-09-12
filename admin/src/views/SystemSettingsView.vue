<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getWindow, updateWindow } from '@/api'

const loading = ref(false)
const saving = ref(false)
const start = ref('14:00')
const end = ref('18:00')

onMounted(async () => {
  loading.value = true
  try {
    const w = await getWindow()
    start.value = w.orderWindowStart
    end.value = w.orderWindowEnd
  } finally {
    loading.value = false
  }
})

async function save() {
  if (!start.value || !end.value) {
    ElMessage.warning('请选择开始与结束时间')
    return
  }
  saving.value = true
  try {
    const w = await updateWindow(start.value, end.value)
    start.value = w.orderWindowStart
    end.value = w.orderWindowEnd
    ElMessage.success('点餐窗口已保存')
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-card shadow="never" class="panel" v-loading="loading">
    <template #header><b>点餐窗口设置</b></template>
    <el-form label-width="100px" class="form">
      <el-form-item label="开始时间">
        <el-time-picker v-model="start" value-format="HH:mm" format="HH:mm" placeholder="如 14:00" />
      </el-form-item>
      <el-form-item label="结束时间">
        <el-time-picker v-model="end" value-format="HH:mm" format="HH:mm" placeholder="如 18:00" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </el-form-item>
    </el-form>
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="窗口为闭区间，含开始与结束时刻；判定以服务器时间（Asia/Shanghai）为准。"
      description="若服务端启动时设置了 LUNCH_WINDOW_START / LUNCH_WINDOW_END 环境变量，则环境变量优先，此处修改不生效。"
    />
  </el-card>
</template>

<style scoped>
.panel {
  border-radius: 10px;
  max-width: 640px;
}
</style>
