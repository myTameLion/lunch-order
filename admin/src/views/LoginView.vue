<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, Right } from '@element-plus/icons-vue'
import { login } from '@/api'
import { setAuthFromLogin } from '@/store/auth'
import { setToken } from '@/utils/cookie'

const router = useRouter()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({
  loginName: '',
  password: '',
})

const rules: FormRules = {
  loginName: [{ required: true, message: '请输入登录名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const result = await login({ loginName: form.loginName.trim(), password: form.password })
    // 写 cookie（path=/，7 天，SameSite=Lax），与员工 web 端同主机共享
    setToken(result.token)
    setAuthFromLogin(result)
    router.push('/')
  } catch {
    // 401（1001 登录失败）已由拦截器 ElMessage.error 展示后端 message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card shadow="always" class="login-card">
      <div class="login-card__head">
        <div class="login-card__logo">
          <el-icon :size="26"><User /></el-icon>
        </div>
        <h1 class="login-card__title">内网点餐系统 · 管理中台</h1>
        <p class="login-card__subtitle">管理员登录后可查看今日看板与历史统计</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleSubmit">
        <el-form-item prop="loginName">
          <el-input v-model="form.loginName" placeholder="登录名" :prefix-icon="User" autocomplete="username" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-card__submit" :loading="submitting" :icon="Right" @click="handleSubmit">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <p class="login-card__tip">提示：与员工端使用同一账号体系，员工已登录时打开中台即为已登录状态</p>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #409eff 0%, #1d6fd8 55%, #15559f 100%);
}

.login-card {
  width: 400px;
  border-radius: 12px;
}

.login-card__head {
  text-align: center;
  margin-bottom: 8px;
}

.login-card__logo {
  width: 52px;
  height: 52px;
  margin: 0 auto 12px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #409eff, #67c23a);
}

.login-card__title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.login-card__subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  color: #909399;
}

.login-card__submit {
  width: 100%;
  letter-spacing: 4px;
}

.login-card__tip {
  margin: 4px 0 0;
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
}
</style>
