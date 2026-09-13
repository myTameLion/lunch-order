# lunch-server · 后台服务（契约版本: 1.4.0）

Spring Boot 3.3.5 + Kotlin 2.0.21。统一认证中心（SSO）+ 点餐业务 API + 统计/Excel 导出 + web/admin 静态托管。**无数据库**，数据落 `$LUNCH_DATA_DIR`（默认 `./data`）。

## 命令

```bash
./gradlew test          # 全部单测 + 集成测试（@SpringBootTest 随机端口 + 临时数据目录）
./gradlew bootRun       # 启动（默认 8080）
./gradlew bootJar       # 打包 build/libs/lunch-server-1.0.0.jar
```

## 端点清单（详见 shared/contracts/api.yaml）

| 端点 | 权限 | 说明 |
|---|---|---|
| POST /api/auth/login | 公开 | 登录签发 JWT（HS256，7 天，含 pwdVer） |
| GET /api/me · PUT /api/me/profile · PUT /api/me/password | 登录 | 个人信息 / 改姓名 / 改密码（旧 token 立即失效） |
| GET·PUT·DELETE /api/orders/today | 登录 | 我的今日点餐（窗口校验 [14:00, 18:00] 闭区间，Asia/Shanghai） |
| GET /api/orders/today/all | ADMIN | 当天名单与辣度统计 |
| GET /api/admin/summary?from=&to= | ADMIN | 区间统计（逐日 + 每人，含 0 值） |
| GET /api/admin/export?from=&to= | ADMIN | 导出 .xlsx（点餐明细 + 人员汇总两个 Sheet） |
| GET·POST /api/admin/users | ADMIN | 用户列表 / 创建用户 |

## 配置

| 环境变量 | 默认 | 说明 |
|---|---|---|
| `LUNCH_DATA_DIR` | `./data` | 数据目录（users.json / orders/*.json / config.json） |
| `LUNCH_JWT_SECRET` | 内置开发值（启动打 warn） | JWT 密钥，≥32 字节，生产必须注入 |
| `LUNCH_WINDOW_START` / `LUNCH_WINDOW_END` | config.json（14:00/18:00） | 窗口覆盖 |
| `LUNCH_PORT` | 8080 | 端口 |

首次启动自动种子初始化：`admin/admin123`（ADMIN，姓名"管理员"）+ 5 个演示账号（zhangsan 张三 / lisi 李四 / wangwu 王五 / zhaoliu 赵六 / sunqi 孙七，密码均 `123456`）。

## 实现要点

- 文件存储：原子写（临时文件 + ATOMIC_MOVE）+ 进程内写锁；启动自举校验，单文件损坏告警并按空处理/重新种子（不静默清空其他数据）；
- 鉴权：Servlet Filter 校验 Bearer token；JWT 内 `pwdVer` 与 users.json 比对实现改密后旧 token 失效（D-004）；admin 接口双重校验（路径 + role）；
- 时间：统一注入 `Clock`（生产固定 Asia/Shanghai），测试可注入固定时钟（D-007）；
- SPA 回退（D-006）：非 `/api` 的无点 GET 路径，`/admin` 前缀转发 `/admin/index.html`，其余转发 `/index.html`；
- CORS 开发期全放开，生产同域托管后不依赖。
