# shared/ 契约变更日志

> 单一事实来源的演进记录。任何模块开始工作前必须先读本文件；
> 修改 `shared/contracts/` 下任何文件时，必须在此顶部追加条目（语义化版本递增，注明是否 Breaking 与影响模块）。
> 契约变更由项目维护者统一评审落盘。

## [1.4.0] - 2026-09-12

- 决策 D-015：新增**重要通知**功能。管理员在中台发布/删除多条通知（标题 ≤50 字 + 内容 ≤500 字）；所有登录客户端在**顶部突出展示**——折叠时只显示最新一条，可展开查看全部。存储：`notices.json`（单文件，自增 id，按 id 倒序展示）。影响：server / web 顶部横幅 / Android 顶部横幅（可展开） / admin 系统设置页。

## [1.3.0] - 2026-09-11

- 决策 D-013（需求变更）：原「当天点餐评价」（星级+评语，F-EVAL-01~03 已废弃）重构为**公共聊天频道**。所有登录用户可查看**当天**聊天记录并发消息（内容 1~200 字，追加式存储）；管理员可按天查看**任意历史日期**记录。存储：`messages/YYYY-MM-DD.json`（追加式）。客户端无历史日期查询入口。影响：server / web / android / admin。
  - Android UI 约定：点餐功能优先，聊天为次要入口（TodayFragment 仅保留频道入口按钮，聊天在独立页面）。
- 决策 D-014：管理员可在中台设置**定时通知**（提醒时间 + 通知标题 + 通知内容），写入 config.json；`GET /api/settings/notify` 供登录客户端同步（Android 拉取后本地调度闹钟，通知开关仍由用户本地控制）。影响：server / admin 系统设置页 / Android。

## [1.2.0] - 2026-09-06

- 决策 D-009：管理员可在线设置点餐窗口（开始/结束时间）。`GET/PUT /api/admin/settings/window`，写入 `config.json`（环境变量覆盖仍优先）。影响：server、admin 新增"系统设置"页。
- 决策 D-010：支持批量创建用户 + 导出全部账户与密码。
  - `POST /api/admin/users/bulk`：逐行校验，批内重复/与现有重复按条目失败，不影响成功条目；
  - **密码可逆存储**：为满足"导出密码"需求，`users.json` 每账号新增 `passwordEnc`（AES-GCM，密钥 `LUNCH_EXPORT_KEY` 注入，缺省开发密钥并告警）；登录校验仍用 BCrypt；历史存量账号无该字段时导出显示"—"；
  - `GET /api/admin/users/export`：xlsx（登录名/姓名/角色/密码/创建时间），仅 ADMIN。**安全权衡**：可逆存储密码天然降低安全性，仅适合内网信任环境。
- 决策 D-011：用户可查自己的历史点餐。`GET /api/me/orders?from=&to=`（缺省最近 30 天，跨度≤366 天），返回逐日记录与汇总。
- 决策 D-012：当天点餐评价。`GET/PUT /api/orders/today/evaluation`（rating 1~5 + 可选评语 ≤100 字；**需当天已登记点餐**，否则 2002；重复提交=修改，每用户每天仅保留最后一次评价）；`GET /api/admin/evaluations?date=` 供中台按天查看全员评价（含平均分）。评价不受点餐窗口限制。
- 后台启动时打印可访问地址（localhost + 局域网 IPv4 + 员工端/中台路径）。
- 各模块 README 契约版本更新为 1.2.0。

## [1.1.0] - 2026-09-05

- 决策 D-008（非 Breaking）：`GET /api/orders/today/all` 由仅 ADMIN 开放为**所有登录用户**可查——所有人都可以实时查看当天的点餐情况（名单/人数/辣度分布）。`/api/admin/**` 仍仅限 ADMIN。
  - 影响模块：server 去掉该端点的角色限制；web 员工端点餐页新增"今日点餐情况"面板（30 秒轮询 + 操作后即时刷新）；Android 今日点餐页新增当天统计摘要；admin 今日看板增加 30 秒轮询保持实时。
  - 各模块 README 契约版本更新为 1.1.0。

## [1.0.0] - 2026-09-05（初版）

- 建立 `contracts/api.yaml`（REST 契约）、`contracts/data-file.md`（本地文件存储格式）、`contracts/error-codes.md`（全局错误码）、`contracts/glossary.md`（领域术语表）。
- 决策 D-001：在原提示语基础上新增 `GET/POST /api/admin/users`（管理员创建/查看账号）。理由：否则新增用户需手工编辑 BCrypt 哈希，系统不可运营。server 必须实现；admin 端提供简单用户管理页。
- 决策 D-002：token 存储约定——web/admin 使用 cookie `lunch_token`（path=/，有效期 7 天，SameSite=Lax），同主机跨端口共享，落地"单点登录"；Android 存 SharedPreferences，请求头 `Authorization: Bearer <token>`。
- 决策 D-003：点餐窗口为闭区间 [14:00, 18:00]（含 14:00:00 与 18:00:00 两个边界时刻），以服务器 Asia/Shanghai 时间判定，客户端时间仅作展示。
- 决策 D-004：修改密码后旧 token 立即失效。实现口径：JWT payload 携带 `pwdVer`，与 `users.json` 中该用户 `pwdVer` 不一致即 401/1002；修改密码成功后返回新 token。
- 决策 D-005：`perUser` 统计包含区间内所有用户（含 0 单用户），便于领导视角查看参与率。
- 决策 D-006：生产部署时员工 web 构建产物由 server 托管在 `/`、管理中台托管在 `/admin/`（均为 Vue history 路由）。server 须实现 SPA 路由回退：非 `/api/` 开头且匹配不到静态资源的 GET 请求，`/admin` 前缀的转发到 `/admin/index.html`，其余转发到 `/index.html`（200）；`/api/**` 不回退。
- 决策 D-007：新增系统级错误码 404（接口不存在）与 5000（服务器内部错误），业务错误码不变；server 以可注入的 `Clock`（固定 Asia/Shanghai）产生业务时间，便于测试注入固定时间。
