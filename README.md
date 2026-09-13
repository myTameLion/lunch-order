# lunch-order · 内网点餐系统

[![CI](https://github.com/myTameLion/lunch-order/actions/workflows/ci.yml/badge.svg)](https://github.com/myTameLion/lunch-order/actions/workflows/ci.yml)

> 📋 **维护者与新成员请优先阅读 [功能清单.md](功能清单.md)**——项目功能唯一权威清单（按模块编号），新增/变更功能时必须同步更新它。

后台 + 员工 Web + 管理中台 + Android 客户端。仅限内网、无数据库（JSON 文件存储），采用「Monorepo + 共享契约变更驱动」的开发模式。

## 模块

| 目录 | 说明 | 技术 |
|---|---|---|
| `shared/` | ★ 共享契约层（api.yaml / 数据文件格式 / 错误码 / 术语表 / CHANGELOG） | 文档即契约，单一事实来源 |
| `server/` | 认证中心（SSO）+ 点餐 API + 统计/Excel 导出 + 静态托管 | Spring Boot 3.3.5 + Kotlin |
| `web/` | 员工点餐端（登记是否点餐、要辣/不要辣、改姓名密码） | Vue 3 + Element Plus |
| `admin/` | 管理中台（今日看板、区间统计图表、导出 Excel、用户管理） | Vue 3 + Element Plus + ECharts |
| `android/` | 客户端（可配服务器地址、登记点餐、每天 17:30 提醒） | Kotlin + XML（禁 Compose） |
| `scripts/` | check-all.sh（全量单测）/ build-all.sh（单 jar 打包）/ reset-data.sh | bash |
| `docs/` | 部署文档、数据说明 | — |

## 快速开始

```bash
# 全量单测
bash scripts/check-all.sh

# 开发：后台 8080 + web 5173 + admin 5174（详见 docs/部署文档.md）
cd server && ./gradlew bootRun
cd web && npm install && npm run dev
cd admin && npm install && npm run dev

# 生产：单 jar 托管 web(/) 与 admin(/admin/)
bash scripts/build-all.sh
java -jar server/build/libs/server-*.jar
```

种子账号：`admin/admin123`（管理员）；演示用户 `zhangsan` 等 5 个，密码 `123456`。

## 核心业务规则

- 点餐窗口每天 **[14:00, 18:00]**（闭区间，服务器 Asia/Shanghai 时间判定）；窗口外不可登记/修改/取消（管理员不受限）；
- 每人每天一条记录，重复提交=修改；登记时选择**要辣 / 不要辣**；
- 单点登录：web 与 admin 同域托管共享 cookie `lunch_token`；Android 同账号体系；
- 中台：今日名单/人数/辣度统计，任意时间段人员统计图表，一键导出 Excel。

## 协作约定（变更驱动）

任何跨模块接口/数据/规则改动：先改 `shared/contracts/`，在 `shared/CHANGELOG.md` 顶部追加版本条目，再动代码；各模块 README 记录所依赖的契约版本（当前 **1.4.1**）。

## 开源协议

本项目基于 [MIT License](LICENSE) 开源。内网点餐场景的演示密码（`admin123`、`123456`）仅为开箱体验设计，生产部署请务必：注入 `LUNCH_JWT_SECRET` / `LUNCH_EXPORT_KEY`、修改全部初始密码。
