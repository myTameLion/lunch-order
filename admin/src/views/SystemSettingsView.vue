<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createNotice, deleteNotice, getNoticeList, getNotifySettings, getWindow, updateNotifySettings, updateWindow } from '@/api'
import type { NoticeItem } from '@/api/types'
import { formatDateTime } from '@/utils/date'

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
    await loadNotices()
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

/** 重要通知管理（D-015） */
const notices = ref<NoticeItem[]>([])
const noticeForm = ref({ title: '', content: '' })
const noticeSubmitting = ref(false)

async function loadNotices() {
  notices.value = await getNoticeList()
}

async function publishNotice() {
  if (!noticeForm.value.title.trim() || !noticeForm.value.content.trim()) {
    ElMessage.warning('请填写通知标题与内容')
    return
  }
  noticeSubmitting.value = true
  try {
    await createNotice(noticeForm.value.title.trim(), noticeForm.value.content.trim())
    ElMessage.success('重要通知已发布')
    noticeForm.value.title = ''
    noticeForm.value.content = ''
    await loadNotices()
  } catch {
    /* 拦截器已提示 */
  } finally {
    noticeSubmitting.value = false
  }
}

async function removeNotice(id: number) {
  await ElMessageBox.confirm('确定删除这条通知吗？', '删除通知', { type: 'warning' })
  await deleteNotice(id)
  ElMessage.success('已删除')
  await loadNotices()
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

    <el-card shadow="never" class="panel notice-panel" style="max-width: none">
      <template #header><b>重要通知管理（客户端顶部突出展示，多条可展开）</b></template>
      <el-form label-width="80px" class="form">
        <el-form-item label="标题">
          <el-input v-model="noticeForm.title" maxlength="50" show-word-limit placeholder="如：周五聚餐报名" style="max-width: 480px" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="noticeForm.content" maxlength="500" show-word-limit type="textarea" :rows="3" placeholder="通知正文（500 字以内）" style="max-width: 480px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="noticeSubmitting" @click="publishNotice">发布通知</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="notices" stripe empty-text="还没有发布过通知">
        <el-table-column prop="id" label="#" width="60" />
        <el-table-column prop="title" label="标题" min-width="140" />
        <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="发布时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="createdBy" label="发布人" width="90" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeNotice(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
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
