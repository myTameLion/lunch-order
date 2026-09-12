<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import type { EvaluationView, TodayAllResponse, TodayStatus } from '../api/types'
import { formatOrderedAt, formatRemain, windowState } from '../utils/window'

const status = ref<TodayStatus | null>(null)
const willOrder = ref(false)
const spicy = ref(false)
const loading = ref(false)

/** 全员实时看板（D-008）：当天点餐情况，30 秒轮询 */
const todayAll = ref<TodayAllResponse | null>(null)
const allLoading = ref(false)
let pollTimer: number | undefined

async function loadAll() {
  allLoading.value = true
  try {
    todayAll.value = await api.getTodayAll()
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    allLoading.value = false
  }
}

/** 当天评价（D-012）：需已登记点餐；重复提交=修改 */
const myEval = ref<EvaluationView | null>(null)
const evalRating = ref(0)
const evalComment = ref('')
const evalSubmitting = ref(false)

async function loadMyEval() {
  try {
    myEval.value = await api.getMyEvaluation()
    if (myEval.value) {
      evalRating.value = myEval.value.rating
      evalComment.value = myEval.value.comment
    }
  } catch {
    /* 拦截器已提示 */
  }
}

async function submitEval() {
  if (evalRating.value === 0) {
    ElMessage.info('请先选择星级')
    return
  }
  evalSubmitting.value = true
  try {
    myEval.value = await api.putEvaluation(evalRating.value, evalComment.value.trim())
    ElMessage.success('评价已提交')
  } catch {
    /* 拦截器已提示（如 2002 未点餐） */
  } finally {
    evalSubmitting.value = false
  }
}

/** 用 serverTime 校准本地时钟偏移 */
let offsetMs = 0
let timer: number | undefined
const nowMs = ref(Date.now())

const state = computed(() => {
  if (!status.value) return null
  return windowState(status.value.window.start, status.value.window.end, nowMs.value)
})
const remainText = computed(() => (state.value ? formatRemain(state.value.remainSeconds) : ''))
const hasOrder = computed(() => !!status.value?.myOrder)

async function load() {
  loading.value = true
  try {
    const s = await api.getToday()
    status.value = s
    const serverMs = new Date(s.window.serverTime).getTime()
    offsetMs = serverMs - Date.now()
    nowMs.value = Date.now() + offsetMs
    willOrder.value = !!s.myOrder
    if (s.myOrder) spicy.value = s.myOrder.spicy
  } finally {
    loading.value = false
  }
}

function tick() {
  const prev = state.value?.phase
  nowMs.value = Date.now() + offsetMs
  if (prev === 'open' && state.value?.phase === 'after') {
    void load()
  }
}

onMounted(() => {
  void load()
  void loadAll()
  void loadMyEval()
  timer = window.setInterval(tick, 1000)
  pollTimer = window.setInterval(loadAll, 30_000)
})
onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
  if (pollTimer) window.clearInterval(pollTimer)
})

async function save() {
  if (!willOrder.value && !hasOrder.value) {
    ElMessage.info('今天不点餐就无需登记啦')
    return
  }
  if (willOrder.value) {
    await api.putOrder(spicy.value)
    ElMessage.success(hasOrder.value ? '已修改今日点餐' : '登记成功')
  } else {
    await ElMessageBox.confirm('确定取消今天的点餐吗？', '取消点餐', { type: 'warning' })
    await api.cancelOrder()
    ElMessage.success('已取消今日点餐')
  }
  await load()
  await loadAll()
}
</script>

<template>
  <el-card v-loading="loading" class="order-card" data-test="order-card">
    <template #header>
      <div class="card-head">
        <span>今日点餐 · {{ status?.date }}</span>
        <el-tag v-if="state?.phase === 'open'" type="success" effect="light">登记进行中</el-tag>
      </div>
    </template>

    <el-alert
      v-if="state?.phase === 'before'"
      type="info"
      :closable="false"
      show-icon
      title="今日登记未开始"
      :description="`每天 ${status?.window.start} 开始登记，${status?.window.end} 截止`"
    />
    <el-alert
      v-else-if="state?.phase === 'after'"
      type="warning"
      :closable="false"
      show-icon
      title="今日登记已截止"
      :description="`登记窗口为每天 ${status?.window.start} - ${status?.window.end}，明天再见`"
    />
    <div v-else-if="state" class="remain">
      距离 {{ status?.window.end }} 截止还有 <b class="count" data-test="remain">{{ remainText }}</b>
    </div>

    <el-form label-width="90px" :disabled="state?.phase !== 'open'" class="form">
      <el-form-item label="要点餐吗">
        <el-switch v-model="willOrder" active-text="要点餐" inactive-text="不点餐" data-test="switch" />
      </el-form-item>
      <el-form-item v-if="willOrder" label="口味">
        <el-radio-group v-model="spicy" data-test="spicy">
          <el-radio :value="false">不要辣</el-radio>
          <el-radio :value="true">🌶️ 要辣</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" data-test="save" :disabled="state?.phase !== 'open'" @click="save">
          保存
        </el-button>
      </el-form-item>
    </el-form>

    <el-divider />
    <div class="state" data-test="state">
      <template v-if="hasOrder && status?.myOrder">
        <el-tag :type="status.myOrder.spicy ? 'danger' : 'success'">
          已登记：{{ status.myOrder.spicy ? '🌶️ 要辣' : '不要辣' }}
        </el-tag>
        <span class="time">登记于 {{ formatOrderedAt(status.myOrder.orderedAt) }}</span>
      </template>
      <span v-else class="none">今日还未登记</span>
    </div>
  </el-card>

  <el-card v-loading="allLoading" class="all-card" data-test="today-all">
    <template #header>
      <div class="card-head">
        <span>今日点餐情况 · {{ todayAll?.date }}</span>
        <span class="refresh-hint">每 30 秒自动刷新</span>
      </div>
    </template>

    <div class="stats">
      <el-tag type="primary" effect="plain" size="large">已订 {{ todayAll?.total ?? 0 }} 份</el-tag>
      <el-tag type="danger" effect="plain" size="large">🌶️ 要辣 {{ todayAll?.spicy ?? 0 }} 份</el-tag>
      <el-tag type="success" effect="plain" size="large">不要辣 {{ todayAll?.nonSpicy ?? 0 }} 份</el-tag>
    </div>

    <el-table v-if="(todayAll?.orders?.length ?? 0) > 0" :data="todayAll!.orders" size="small" class="list">
      <el-table-column prop="displayName" label="姓名" min-width="90" />
      <el-table-column label="辣度" width="100">
        <template #default="{ row }">
          <el-tag :type="row.spicy ? 'danger' : 'success'" size="small" effect="light">
            {{ row.spicy ? '🌶️' : '不辣' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="登记时间" min-width="110">
        <template #default="{ row }">{{ formatOrderedAt(row.orderedAt) }}</template>
      </el-table-column>
    </el-table>
    <el-empty v-else description="今天还没有人点餐" :image-size="60" />
  </el-card>

  <el-card class="eval-card" data-test="eval">
    <template #header>
      <div class="card-head">
        <span>今日点餐评价</span>
        <span v-if="myEval" class="refresh-hint">已于 {{ formatOrderedAt(myEval.ratedAt) }} 评价，可修改</span>
      </div>
    </template>
    <el-alert v-if="!hasOrder" type="info" :closable="false" title="登记今天的点餐后即可评价" />
    <template v-else>
      <div class="eval-row">
        <el-rate v-model="evalRating" data-test="rate" />
        <el-button type="primary" size="small" :loading="evalSubmitting" data-test="eval-submit" @click="submitEval">
          提交评价
        </el-button>
      </div>
      <el-input
        v-model="evalComment"
        type="textarea"
        :rows="2"
        maxlength="100"
        show-word-limit
        placeholder="可选：聊聊今天的饭菜（100 字以内）"
        data-test="eval-comment"
        class="eval-input"
      />
    </template>
  </el-card>
</template>

<style scoped>
.order-card {
  border-radius: 10px;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.remain {
  color: #606266;
  margin-bottom: 14px;
}
.count {
  color: #e6a23c;
  font-size: 18px;
  font-variant-numeric: tabular-nums;
}
.form {
  max-width: 420px;
}
.state {
  color: #606266;
  display: flex;
  gap: 10px;
  align-items: center;
}
.none {
  color: #909399;
}
.all-card {
  border-radius: 10px;
  margin-top: 18px;
}
.eval-card {
  border-radius: 10px;
  margin-top: 18px;
}
.eval-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}
.eval-input {
  max-width: 560px;
}
.refresh-hint {
  font-size: 12px;
  color: #c0c4cc;
  font-weight: 400;
}
.stats {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}
.list {
  width: 100%;
}
</style>
