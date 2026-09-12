<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createUser, exportUsers, bulkCreateUsers, getUsers } from '@/api'
import type { Role, UserView } from '@/api/types'
import { formatDateTime } from '@/utils/date'
import { downloadBlob, parseFilenameFromDisposition } from '@/utils/download'
import { parseBulkUsers } from '@/utils/bulk'
import { isValidDisplayName, isValidLoginName, isValidPassword, isValidRole } from '@/utils/validate'

const loading = ref(false)
const users = ref<UserView[]>([])
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance | null>(null)

/** 批量创建（D-010） */
const bulkVisible = ref(false)
const bulkSubmitting = ref(false)
const bulkText = ref('')
const bulkDefaultPassword = ref('123456')
const bulkResult = ref<{ created: number; failed: { loginName: string; reason: string }[] } | null>(null)
const bulkErrors = ref<string[]>([])

/** 导出账户（D-010） */
const exporting = ref(false)

const form = reactive<{ loginName: string; displayName: string; password: string; role: Role }>({
  loginName: '',
  displayName: '',
  password: '',
  role: 'USER',
})

const rules: FormRules = {
  loginName: [
    { required: true, message: '请输入登录名', trigger: 'blur' },
    { validator: (_r, value: string, cb) => cb(isValidLoginName(value ?? '') ? undefined : new Error('3~20 位字母/数字/下划线')), trigger: 'blur' },
  ],
  displayName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { validator: (_r, value: string, cb) => cb(isValidDisplayName(value ?? '') ? undefined : new Error('1~20 个非空白字符')), trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { validator: (_r, value: string, cb) => cb(isValidPassword(value ?? '') ? undefined : new Error('密码长度需为 6~64 位')), trigger: 'blur' },
  ],
  role: [{ validator: (_r, value: Role, cb) => cb(isValidRole(value) ? undefined : new Error('角色非法')), trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    users.value = await getUsers()
  } finally {
    loading.value = false
  }
}
onMounted(load)

function openDialog() {
  form.loginName = ''
  form.displayName = ''
  form.password = ''
  form.role = 'USER'
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate().catch(() => Promise.reject(new Error('invalid')))
  submitting.value = true
  try {
    await createUser({ ...form })
    ElMessage.success(`已创建用户 ${form.displayName}`)
    dialogVisible.value = false
    await load()
  } catch {
    /* 1005 等错误已由拦截器弹出后端 message */
  } finally {
    submitting.value = false
  }
}

function openBulkDialog() {
  bulkText.value = ''
  bulkResult.value = null
  bulkErrors.value = []
  bulkVisible.value = true
}

async function submitBulk() {
  const parsed = parseBulkUsers(bulkText.value, bulkDefaultPassword.value || '123456')
  bulkErrors.value = parsed.errors
  bulkResult.value = null
  if (parsed.users.length === 0) {
    ElMessage.warning(parsed.errors.length > 0 ? '存在格式错误的行，请修正后再提交' : '请先粘贴要创建的用户列表')
    return
  }
  bulkSubmitting.value = true
  try {
    const resp = await bulkCreateUsers(parsed.users)
    bulkResult.value = resp
    await load()
    if (resp.failed.length === 0) {
      ElMessage.success(`批量创建成功 ${resp.created} 个账号`)
    }
  } catch {
    /* 拦截器已提示 */
  } finally {
    bulkSubmitting.value = false
  }
}

async function doExport() {
  exporting.value = true
  try {
    const resp = await exportUsers()
    const name = parseFilenameFromDisposition(resp.headers?.['content-disposition']) ?? `账号列表_${new Date().toISOString().slice(0, 10)}.xlsx`
    downloadBlob(resp.data, name)
    ElMessage.success('账户列表已导出')
  } finally {
    exporting.value = false
  }
}
</script>

<template>
  <el-card shadow="never" class="panel" v-loading="loading">
    <template #header>
      <div class="head">
        <b>用户管理（{{ users.length }} 人）</b>
        <div class="head-actions">
          <el-button @click="openBulkDialog">批量创建</el-button>
          <el-button :loading="exporting" @click="doExport">导出账户</el-button>
          <el-button type="primary" @click="openDialog">新增用户</el-button>
        </div>
      </div>
    </template>

    <el-table :data="users" stripe>
      <el-table-column prop="displayName" label="姓名" min-width="120" />
      <el-table-column prop="loginName" label="登录名" min-width="120" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'" effect="light">
            {{ row.role === 'ADMIN' ? '管理员' : '员工' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="160">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增用户" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="登录名" prop="loginName">
          <el-input v-model="form.loginName" placeholder="3~20 位字母/数字/下划线，创建后不可修改" />
        </el-form-item>
        <el-form-item label="姓名" prop="displayName">
          <el-input v-model="form.displayName" maxlength="20" placeholder="展示用姓名" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio value="USER">员工</el-radio>
            <el-radio value="ADMIN">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bulkVisible" title="批量创建用户" width="640px">
      <el-form label-width="90px">
        <el-form-item label="默认密码">
          <el-input v-model="bulkDefaultPassword" placeholder="未填写密码的行使用该密码（至少 6 位）" style="max-width: 240px" />
        </el-form-item>
        <el-form-item label="用户列表">
          <el-input
            v-model="bulkText"
            type="textarea"
            :rows="9"
            placeholder="每行一条：登录名,姓名[,密码]&#10;示例：&#10;student01,张三&#10;student02,李四,abc12345&#10;（也支持中文逗号或空格分隔）"
          />
        </el-form-item>
      </el-form>

      <el-alert
        v-if="bulkErrors.length > 0"
        type="error"
        :closable="false"
        class="bulk-alert"
        :title="`有 ${bulkErrors.length} 行格式错误，请修正后再提交`"
      >
        <div v-for="e in bulkErrors" :key="e">{{ e }}</div>
      </el-alert>

      <el-result
        v-if="bulkResult"
        icon="success"
        title="批量创建完成"
        :sub-title="`成功创建 ${bulkResult.created} 个账号${bulkResult.failed.length > 0 ? `，失败 ${bulkResult.failed.length} 条` : ''}`"
      >
        <template #extra v-if="bulkResult.failed.length > 0">
          <el-alert type="warning" :closable="false" title="失败明细">
            <div v-for="f in bulkResult.failed" :key="f.loginName">{{ f.loginName }}：{{ f.reason }}</div>
          </el-alert>
        </template>
      </el-result>

      <template #footer>
        <el-button @click="bulkVisible = false">关闭</el-button>
        <el-button type="primary" :loading="bulkSubmitting" @click="submitBulk">解析并创建</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.panel {
  border-radius: 10px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.head-actions {
  display: flex;
  gap: 0;
}
.bulk-alert {
  margin-bottom: 8px;
}
</style>
