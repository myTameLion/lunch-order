<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Lock } from '@element-plus/icons-vue'
import { resetAuth } from '@/store/auth'
import { clearToken } from '@/utils/cookie'

const router = useRouter()

function backToLogin() {
  clearToken()
  resetAuth()
  router.push('/login')
}
</script>

<template>
  <div class="forbidden">
    <el-result icon="warning" title="403" sub-title="需要管理员权限，当前账号无法访问管理中台">
      <template #icon>
        <el-icon :size="72" color="#E6A23C"><Lock /></el-icon>
      </template>
      <template #extra>
        <el-button type="primary" @click="backToLogin">切换账号登录</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.forbidden {
  display: flex;
  justify-content: center;
  padding-top: 18vh;
}
</style>
