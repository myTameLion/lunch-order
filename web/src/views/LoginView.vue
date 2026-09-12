<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { setToken } from '../utils/cookie'
import { loadUser } from '../store/user'

const route = useRoute()
const router = useRouter()

const form = reactive({ loginName: '', password: '' })
const loading = ref(false)

async function submit() {
  if (!form.loginName.trim() || !form.password) {
    ElMessage.warning('请输入登录名和密码')
    return
  }
  loading.value = true
  try {
    const result = await api.login(form.loginName.trim(), form.password)
    setToken(result.token)
    await loadUser()
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.push(redirect)
  } catch {
    /* 错误信息由拦截器统一弹出 */
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2 class="title">🍱 内网点餐系统</h2>
      <p class="subtitle">登录后登记今日点餐</p>
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="登录名">
          <el-input v-model="form.loginName" placeholder="登录名" data-test="loginName" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" data-test="password" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" data-test="submit" @click="submit">登 录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-wrap {
  display: flex;
  justify-content: center;
  padding-top: 12vh;
}
.login-card {
  width: 380px;
  border-radius: 10px;
}
.title {
  text-align: center;
  margin: 4px 0 0;
  color: #303133;
}
.subtitle {
  text-align: center;
  color: #909399;
  margin: 6px 0 18px;
}
.submit {
  width: 100%;
}
</style>
