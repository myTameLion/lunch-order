<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import NoticeBanner from './components/NoticeBanner.vue'
import { userStore, logout } from './store/user'
import { getToken } from './utils/cookie'

const router = useRouter()
onMounted(async () => {
  if (getToken() && !userStore.loaded) {
    const { loadUser } = await import('./store/user')
    await loadUser()
  }
})
</script>

<template>
  <el-container class="layout">
    <el-header class="header" height="56px">
      <div class="brand" @click="router.push('/')">🍱 内网点餐</div>
      <div v-if="userStore.loaded" class="user">
        <span class="name">{{ userStore.displayName }}</span>
        <el-button link type="primary" data-test="logout" @click="logout">退出登录</el-button>
      </div>
    </el-header>
    <NoticeBanner />
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<style>
body {
  margin: 0;
  background: #f5f7fa;
  font-family: 'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.layout {
  min-height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  position: sticky;
  top: 0;
  z-index: 10;
}
.brand {
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
  cursor: pointer;
}
.user {
  display: flex;
  align-items: center;
  gap: 10px;
}
.name {
  color: #606266;
}
.main {
  max-width: 760px;
  margin: 0 auto;
  width: 100%;
}
</style>
