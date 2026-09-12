import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { getToken } from './utils/cookie'
import { loadUser } from './store/user'

const app = createApp(App)
app.use(ElementPlus)
app.use(router)

// 已登录则水合用户信息（供顶部栏展示）
if (getToken()) {
  void loadUser()
}

app.mount('#app')
