# lunch-web · 员工点餐端（契约版本: 1.5.0）

## 命令

```bash
npm install
npm run dev     # http://localhost:5173，/api 代理到 http://localhost:8080
npm test        # vitest run（jsdom）
npm run build   # 产物 dist/，base=/
```

## 页面

- `/login` 登录（SSO 入口）
- `/` 今日点餐：窗口倒计时、要点餐开关、要辣/不要辣、保存/取消
- `/profile` 我的：改姓名、改密码

## 单点登录（D-002）

token 存 cookie `lunch_token`（path=/，7 天，SameSite=Lax）。与 `admin/` 中台同主机部署时共享该 cookie：在任一端登录，另一端即已登录；任意端 401 统一清 cookie 跳 `/login`（中台为 `/admin/login`）。

## 代理配置

开发期 Vite 把 `/api` 代理到 `http://localhost:8080`（vite.config.ts）。生产构建产物由后台同域托管在 `/`（见 scripts/build-all.sh）。
