# 领域术语表 v1.0.0

| 术语 | 字段/取值 | 定义 |
|---|---|---|
| 登录名 | `loginName`，`^[a-zA-Z0-9_]{3,20}$` | 唯一登录凭据，**用户不可自助修改**，仅管理员创建/管理 |
| 用户姓名 | `displayName`，1~20 个非空白字符 | 展示字段，用户可自助修改；所有界面展示一律用它 |
| 角色 | `role`：`USER` \| `ADMIN` | ADMIN 可访问中台与用户管理接口 |
| 要辣 | `spicy: true` | 当天点餐要辣椒 |
| 不要辣 | `spicy: false` | 当天点餐不要辣椒 |
| 点餐窗口 | `[orderWindowStart, orderWindowEnd]`，默认 [14:00, 18:00] | 闭区间，含两端边界时刻；服务器 Asia/Shanghai 时间 |
| 点餐人数 | 当日去重 `loginName` 数 | 统计口径唯一标准 |
| 参与率 | 某人区间内点餐天数 ÷ 区间自然日数 | 区间含 from 与 to 两天；分母为 0 时取 0 |
| 契约版本 | `shared/VERSION`，当前 1.0.0 | 各模块 README 必须记录所依赖版本 |

## 命名约定

- 时间字符串：文件内 ISO 8601 带时区（如 `2026-09-05T14:23:05+08:00`）；日期 `yyyy-MM-dd`；时刻 `HH:mm`。
- 布尔一律 `true/false`，不用 0/1。
- JSON 字段 camelCase；后端 Kotlin data class、前端 TS interface、Android @Serializable 一一对应，字段名不得私改。
