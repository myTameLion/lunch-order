<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import type { MyOrdersResponse } from '../api/types'
import { userStore } from '../store/user'
import { setToken } from '../utils/cookie'
import { formatOrderedAt } from '../utils/window'
import { validateConfirm, validateDisplayName, validatePassword } from '../utils/validate'

const profile = reactive({ loginName: '', displayName: '' })
const pwd = reactive({ old: '', next: '', confirm: '' })
const savingName = ref(false)
const savingPwd = ref(false)

/** 我的历史点餐（D-011）：默认最近 30 天 */
const history = ref<MyOrdersResponse | null>(null)
const historyLoading = ref(false)
const historyRange = ref<[string, string]>(defaultHistoryRange())

function fmtDate(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function defaultHistoryRange(): [string, string] {
  const to = new Date()
  const from = new Date()
  from.setDate(from.getDate() - 29)
  return [fmtDate(from), fmtDate(to)]
}

async function loadHistory() {
  historyLoading.value = true
  try {
    history.value = await api.getMyOrders(historyRange.value[0], historyRange.value[1])
  } finally {
    historyLoading.value = false
  }
}

function historyRangeDays(): number {
  const [f, t] = historyRange.value
  const days = Math.round((new Date(t).getTime() - new Date(f).getTime()) / 86_400_000) + 1
  return days > 0 ? days : 0
}

onMounted(async () => {
  const me = await api.me()
  profile.loginName = me.loginName
  profile.displayName = me.displayName
  userStore.displayName = me.displayName
  await loadHistory()
})

async function saveName() {
  if (!validateDisplayName(profile.displayName)) {
    ElMessage.warning('姓名需为 1~20 个字符')
    return
  }
  savingName.value = true
  try {
    await api.updateProfile(profile.displayName.trim())
    userStore.displayName = profile.displayName.trim()
    ElMessage.success('姓名已更新')
  } finally {
    savingName.value = false
  }
}

async function savePwd() {
  if (!validatePassword(pwd.next)) {
    ElMessage.warning('新密码长度需为 6~64 位')
    return
  }
  if (!validateConfirm(pwd.next, pwd.confirm)) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  savingPwd.value = true
  try {
    const resp = await api.changePassword(pwd.old, pwd.next)
    setToken(resp.token)
    ElMessage.success('密码已修改')
    pwd.old = ''
    pwd.next = ''
    pwd.confirm = ''
  } finally {
    savingPwd.value = false
  }
}
</script>

<template>
  <el-card class="card">
    <template #header><b>我的账户</b></template>
    <el-form label-width="90px" class="form">
      <el-form-item label="登录名">
        <el-input v-model="profile.loginName" disabled data-test="loginName" />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="profile.displayName" maxlength="20" data-test="displayName" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="savingName" data-test="saveName" @click="saveName">保存姓名</el-button>
      </el-form-item>
    </el-form>
  </el-card>

  <el-card class="card">
    <template #header><b>修改密码</b></template>
    <el-form label-width="90px" class="form">
      <el-form-item label="旧密码">
        <el-input v-model="pwd.old" type="password" show-password data-test="oldPwd" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="pwd.next" type="password" show-password placeholder="至少 6 位" data-test="newPwd" />
      </el-form-item>
      <el-form-item label="确认密码">
        <el-input v-model="pwd.confirm" type="password" show-password data-test="confirmPwd" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="savingPwd" data-test="savePwd" @click="savePwd">修改密码</el-button>
      </el-form-item>
    </el-form>
  </el-card>

  <el-card class="card" data-test="history">
    <template #header><b>我的历史点餐</b></template>
    <div class="history-bar">
      <el-date-picker
        v-model="historyRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        :clearable="false"
      />
      <el-button type="primary" :loading="historyLoading" data-test="history-query" @click="loadHistory">查询</el-button>
    </div>
    <template v-if="history">
      <el-alert
        type="info"
        :closable="false"
        class="history-summary"
        :title="`${history.from} 至 ${history.to}（${historyRangeDays()} 天）：点餐 ${history.totalDays} 天，要辣 ${history.spicyDays} 天，不要辣 ${history.nonSpicyDays} 天`"
      />
      <el-table v-if="history.records.length > 0" :data="history.records" size="small">
        <el-table-column prop="date" label="日期" min-width="110" />
        <el-table-column label="辣度" width="100">
          <template #default="{ row }">
            <el-tag :type="row.spicy ? 'danger' : 'success'" size="small" effect="light">
              {{ row.spicy ? '🌶️ 要辣' : '不辣' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="登记时间" min-width="110">
          <template #default="{ row }">{{ formatOrderedAt(row.orderedAt) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="该时间段内没有点餐记录" :image-size="60" />
    </template>
  </el-card>
</template>

<style scoped>
.card {
  border-radius: 10px;
  margin-bottom: 18px;
}
.form {
  max-width: 460px;
}
.history-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
.history-summary {
  margin-bottom: 12px;
}
</style>
