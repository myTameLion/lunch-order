import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { auth, hydrateAuth, resetAuth } from '@/store/auth'
import { getToken } from '@/utils/cookie'

const router = createRouter({
  // base = '/admin/'，浏览器地址形如 /admin/login、/admin/history
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      component: AdminLayout,
      children: [
        {
          path: '',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: { title: '今日看板' },
        },
        {
          path: 'history',
          name: 'history',
          component: () => import('@/views/HistoryView.vue'),
          meta: { title: '历史统计' },
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/UsersView.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'chat',
          name: 'chat',
          component: () => import('@/views/ChatRecordsView.vue'),
          meta: { title: '聊天记录' },
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('@/views/SystemSettingsView.vue'),
          meta: { title: '系统设置' },
        },
      ],
    },
    {
      path: '/403',
      name: 'forbidden',
      component: () => import('@/views/ForbiddenView.vue'),
      meta: { title: '无权访问' },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach(async (to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} · 内网点餐系统管理中台` : '内网点餐系统管理中台'

  // 登录页始终放行（已登录用户停留在登录页也无妨，可再次登录切换账号）
  if (to.path === '/login') return true

  // 无 token → 登录页
  if (!getToken()) {
    resetAuth()
    return { path: '/login' }
  }

  // 403 页：只要有 token 即可渲染（供非管理员账号查看提示）
  if (to.path === '/403') return true

  // 有 token 先 GET /api/me 水合身份
  if (!auth.loaded) {
    try {
      await hydrateAuth()
    } catch {
      // 401 已由拦截器清 token 并跳登录；此处兜底
      return { path: '/login' }
    }
  }

  // role !== 'ADMIN' → 403 页
  if (auth.role !== 'ADMIN') {
    return { path: '/403' }
  }
  return true
})

export default router
