# lunch-order-android · Android 客户端（契约版本: 1.4.0）

Kotlin 2.0.21 + XML（ViewBinding，**禁用 Compose**）+ Retrofit/OkHttp/kotlinx-serialization。minSdk 26 / targetSdk 34 / compileSdk 34。

## 命令

```bash
./gradlew :app:testDebugUnitTest   # 纯 JVM 单测（MockWebServer + 固定时钟）
./gradlew :app:assembleDebug       # APK: app/build/outputs/apk/debug/app-debug.apk
```

需要：`JAVA_HOME`（JDK 17+）与 `ANDROID_HOME`（android-34）。本机可用 `source ~/tools/env.sh`。

## 功能

- **登录**：登录名 + 密码 → `POST /api/auth/login`，token/role/displayName 存 SharedPreferences；401(1001) 提示"登录名或密码错误"
- **今日点餐**：窗口状态横幅（未开始/进行中/已截止）、剩余时间倒计时（按 serverTime 校准）、要辣/不要辣登记、修改、取消（对话框确认）；业务错误按 `{code,message}` Toast（如 2001 非点餐时间、2002 无记录）
- **我的**：登录名只读、改姓名、改密码（成功后保存新 token，D-004）、退出登录
- **设置**：服务器地址可配置（自动补 http://、保存后"测试连接"——任意 HTTP 响应即可达）；每日提醒开关 + 时间（默认 17:30）
- **登录页配置 baseURL**：登录前即可展开"服务器设置"修改地址并测试连接（契约 v1.1.0 起内置），避免默认地址不可达时无法进入应用的死循环
- **每日提醒**：`AlarmManager.setExactAndAllowWhileIdle` 精确触发（Android 12+ 无精确闹钟权限时降级并引导去系统设置）；通知点击直达主界面；`BootReceiver` 开机重注册；App 启动/设置变更后自动重排

## 测试

纯 JVM 单测，不依赖 Robolectric：
- `ReminderTime` 下次触发计算（当天已过→明天、恰好相等→明天、跨月/跨年）、`TimeMath` 窗口换算与时长格式化
- `ServerUrl` 规范化与合法性
- `LunchRepository` + Retrofit + MockWebServer 全链路：登录/点餐/取消/改密解析、错误体 `{code,message}` → `ApiException`、401 全局登出事件、X-No-Auth 登录不带 Bearer
- `PrefsStore` 语义（clearSession 保留服务器地址与提醒设置）
