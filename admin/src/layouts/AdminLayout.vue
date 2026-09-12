<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Avatar, ChatDotRound, Dish, Odometer, Setting, SwitchButton, TrendCharts } from '@element-plus/icons-vue'
import { auth, resetAuth } from '@/store/auth'
import { clearToken } from '@/utils/cookie'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => (route.path === '/' ? '/' : route.path))
const pageTitle = computed(() => (route.meta.title as string) ?? '')

function handleCommand(command: string) {
  if (command === 'logout') {
    clearToken()
    resetAuth()
    router.push('/login')
  }
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="admin-layout__aside">
      <div class="admin-layout__brand">
        <el-icon :size="22" color="#409EFF"><Dish /></el-icon>
        <span>内网点餐系统</span>
      </div>
      <el-menu :default-active="activeMenu" router background-color="#001529" text-color="rgba(255,255,255,0.68)" active-text-color="#ffffff" class="admin-layout__menu">
        <el-menu-item index="/">
          <el-icon><Odometer /></el-icon>
          <span>今日看板</span>
        </el-menu-item>
        <el-menu-item index="/history">
          <el-icon><TrendCharts /></el-icon>
          <span>历史统计</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><Avatar /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>聊天记录</span>
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header height="56px" class="admin-layout__header">
        <div class="admin-layout__crumb">{{ pageTitle }}</div>
        <el-dropdown trigger="click" @command="handleCommand">
          <span class="admin-layout__user">
            <el-avatar :size="28" class="admin-layout__avatar">{{ auth.displayName.slice(0, 1) || '管' }}</el-avatar>
            <span>{{ auth.displayName || '未登录' }}</span>
            <el-tag v-if="auth.role === 'ADMIN'" size="small" type="warning" effect="light" round>管理员</el-tag>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="admin-layout__main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  height: 100%;
}

.admin-layout__aside {
  background-color: #001529;
  display: flex;
  flex-direction: column;
}

.admin-layout__brand {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  flex-shrink: 0;
}

.admin-layout__menu {
  border-right: none;
  flex: 1;
}

.admin-layout__menu :deep(.el-menu-item.is-active) {
  background-color: #409eff;
}

.admin-layout__menu :deep(.el-menu-item:hover) {
  background-color: rgba(64, 158, 255, 0.55);
}

.admin-layout__header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 5;
}

.admin-layout__crumb {
  font-size: 15px;
  font-weight: 600;
}

.admin-layout__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.admin-layout__avatar {
  background-color: #409eff;
  color: #fff;
  font-size: 14px;
}

.admin-layout__main {
  padding: 16px;
  overflow: auto;
}
</style>
