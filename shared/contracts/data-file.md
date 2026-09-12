# 本地文件存储契约 v1.0.0

无数据库。全部业务数据以 JSON 文件落盘，UTF-8 编码。

## 目录布局

```
$LUNCH_DATA_DIR/            # 环境变量，默认 ./data
├── users.json              # 全部用户账号
├── config.json             # 运行配置
└── orders/
    ├── 2026-09-05.json     # 每天一个文件（yyyy-MM-dd）
    └── 2026-09-06.json
```

## users.json

```json
{
  "users": [
    {
      "loginName": "zhangsan",
      "displayName": "张三",
      "passwordHash": "$2a$10$...",
      "pwdVer": 1,
      "role": "USER",
      "createdAt": "2026-09-05T10:00:00+08:00",
      "updatedAt": "2026-09-05T10:00:00+08:00"
    }
  ]
}
```

- `passwordHash`：BCrypt；`pwdVer`：密码版本号，修改密码 +1（用于令牌失效，见 CHANGELOG D-004）。
- 文件缺失/为空结构时：服务端启动执行**种子初始化**——写入 `admin/admin123`（ADMIN，姓名"管理员"）与 5 个演示用户 `zhangsan 张三 / lisi 李四 / wangwu 王五 / zhaoliu 赵六 / sunqi 孙七`（密码均 `123456`，USER），并输出日志。

## orders/YYYY-MM-DD.json

```json
{
  "date": "2026-09-05",
  "orders": [
    { "loginName": "zhangsan", "displayName": "张三", "spicy": true, "orderedAt": "2026-09-05T14:23:05+08:00" }
  ]
}
```

- `orders` 按 `orderedAt` 升序；每人每天最多一条，重复提交=修改原记录（`orderedAt` 更新为最后一次操作时间）。
- 当天无记录时文件可以不存在（读取按空处理）。

## config.json

```json
{ "orderWindowStart": "14:00", "orderWindowEnd": "18:00", "notifyTime": "17:30" }
```

- 缺失时按默认值创建；环境变量覆盖优先级最高：`LUNCH_WINDOW_START` / `LUNCH_WINDOW_END`（HH:mm）。
- `notifyTime` 为 Android 端提醒时间的默认参考值（Android 端本地可自行调整）。

## 写入规则（红线）

1. **原子写**：先写同目录临时文件，再 `Files.move(tmp, target, ATOMIC_MOVE)`，禁止直接覆写目标文件；
2. **进程内写锁**：对数据目录全局单锁串行化写操作，防止交错损坏；
3. **自愈**：启动时校验文件可解析；单文件损坏 → 输出告警并按空结构处理（users.json 损坏则走种子初始化），不静默清空其他文件；
4. **备份**：直接拷贝 `$LUNCH_DATA_DIR` 即全量备份（原子写保证单文件完整性）。
