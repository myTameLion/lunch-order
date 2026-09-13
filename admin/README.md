# lunch-admin · 管理中台（契约版本: 1.4.0）

Vue 3 + TypeScript + Vite 5 + Element Plus + ECharts 5。管理员看今日点餐概况、任意时间段统计图表、导出 Excel、用户管理。

## 命令

```bash
npm install
npm run dev     # http://localhost:5174，/api 代理到 http://localhost:8080
npm test        # vitest run（tests/ 目录，jsdom）
npm run build   # base=/admin/，产物 dist/
```

## 页面

- `/admin/login` 登录（与员工端共享 cookie `lunch_token`，单点登录 D-002）
- `/admin/` 今日看板：点餐人数/要辣/不要辣统计牌、辣度环形图、分时段登记趋势折线图（14:00-18:00 每 10 分钟一桶）、名单表格（辣度筛选）
- `/admin/history` 历史统计：日期范围 → 每日人数堆叠柱状图、点餐天数 TOP10、人员汇总表（参与率）、一键导出 Excel（文件名解析 RFC 5987 filename*）
- `/admin/users` 用户管理：列表 + 新增（loginName 正则、密码≥6、角色选择）
- `/403` 非 ADMIN 角色的提示页

## 架构约定

- 图表数据加工与 ECharts option 全部是纯函数（`src/utils/chartData.ts`），Vitest 覆盖边界（14:00/18:00 落桶、窗口外不计、空数据空态）；
- axios 拦截器统一处理 401（清 cookie → `/admin/login`）与业务错误 `{code,message}`（ElMessage 提示）；
- 路由守卫：无 token → 登录页；有 token 先 `GET /api/me` 水合身份；role≠ADMIN → `/403`。

## 与后台同域托管

生产由后台托管在 `/admin/` 路径（`scripts/build-all.sh` 会把 dist 拷入 server 静态目录），SPA 子路由刷新由后台回退到 `/admin/index.html`（契约 D-006）。
