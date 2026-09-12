<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getNotifySettings, getWindow, updateNotifySettings, updateWindow } from '@/api'

const loading = ref(false)
const savingWindow = ref(false)
const start = ref('14:00')
const end = ref('18:00')

/** 定时通知（D-014） */
const savingNotify = ref(false)
const notifyTime = ref('17:30')
const notifyTitle = ref('午餐点餐提醒')
const notifyContent = ref('今天需要点餐吗？请在 18:00 前登记')

onMounted(async () => {
  loading.value = true
  try {
    const [w, n] = await Promise.all([getWindow(), getNotifySettings()])
    start.value = w.orderWindowStart
    end.value = w.orderWindowEnd
    notifyTime.value = n.notifyTime
    notifyTitle.value = n.notifyTitle
    notifyContent.value = n.notifyContent
  } finally {
    loading.value = false
  }
})

async function saveWindow() {
  if (!start.value || !end.value) {
    ElMessage.warning('请选择开始与结束时间')
    return
  }
  savingWindow.value = true
  try {
    const w = await updateWindow(start.value, end.value)
    start.value = w.orderWindowStart
    end.value = w.orderWindowEnd
    ElMessage.success('点餐窗口已保存')
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    savingWindow.value = false
  }
}

async function saveNotify() {
  savingNotify.value = true
  try {
    const n = await updateNotifySettings(notifyTime.value, notifyTitle.value, notifyContent.value)
    notifyTime.value = n.notifyTime
    notifyTitle.value = n.notifyTitle
    notifyContent.value = n.notifyContent
    ElMessage.success('定时通知已保存，客户端将在下次启动/同步后生效')
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    savingNotify.value = false
  }
}
</script>

<template>
  <div v-loading="loading">
    <el-card shadow="never" class="panel">
      <template #header><b>点餐窗口设置</b></template>
      <el-form label-width="100px" class="form">
        <el-form-item label="开始时间">
          <el-time-picker v-model="start" value-format="HH:mm" format="HH:mm" placeholder="如 14:00" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-picker v-model="end" value-format="HH:mm" format="HH:mm" placeholder="如 18:00" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingWindow" @click="saveWindow">保存</el-button>
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

    <el-card shadow="never" class="panel notify">
      <template #header><b>定时通知设置（Android 客户端）</b></template>
      <el-form label-width="100px" class="form">
        <el-form-item label="提醒时间">
          <el-time-picker v-model="notifyTime" value-format="HH:mm" format="HH:mm" placeholder="如 17:30" />
        </el-form-item>
        <el-form-item label="通知标题">
          <el-input v-model="notifyTitle" maxlength="30" show-word-limit placeholder="如 午餐点餐提醒" />
        </el-form-item>
        <el-form-item label="通知内容">
          <el-input v-model="notifyContent" maxlength="60" show-word-limit type="textarea" :rows="2" placeholder="如 今天需要点餐吗？请在 18:00 前登记" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingNotify" @click="saveNotify">保存</el-button>
        </el-form-item>
      </el-form>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="保存后，Android 客户端在下次启动/登录时同步该配置并按新时间调度本地提醒。"
        description="提醒开关由用户在客户端本地控制；未开启提醒的用户不会收到通知。"
      />
    </el-card>
  </div>
</template>

<style scoped>
.panel {
  border-radius: 10px;
  max-width: 720px;
}
.notify {
  margin-top: 16px;
}
</style>
