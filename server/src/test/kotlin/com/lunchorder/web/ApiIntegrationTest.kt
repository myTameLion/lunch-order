package com.lunchorder.web

import com.lunchorder.LunchServerApplication
import com.lunchorder.auth.JwtService
import com.lunchorder.LunchProperties
import com.lunchorder.util.TimeUtil
import com.lunchorder.web.dto.ErrorResponse
import com.lunchorder.web.dto.LoginResponse
import com.lunchorder.web.dto.TodayStatusResponse
import com.lunchorder.web.dto.UserView
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference

/** 测试时钟：窗口内 15:00（+08:00），日期可按用例切换，实现数据目录的按日隔离 */
@TestConfiguration
class MutableTestClockConfig {
    companion object {
        val instant = AtomicReference(Instant.parse("2026-09-07T07:00:00Z"))
    }

    @Bean
    @Primary
    fun testClock(): Clock = object : Clock() {
        override fun getZone(): ZoneId = TimeUtil.SHANGHAI
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = instant.get()
    }
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MutableTestClockConfig::class)
@TestMethodOrder(MethodOrderer.Random::class)
class ApiIntegrationTest {

    @Autowired
    lateinit var rest: TestRestTemplate

    companion object {
        @TempDir
        @JvmStatic
        lateinit var temp: Path

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("lunch.data-dir") { temp.toString() }
            registry.add("lunch.jwt-secret") { "0123456789abcdef0123456789abcdef" }
        }
    }

    /** 每个用例独占一个业务日期（15:00，窗口内），避免共享数据目录下的相互干扰 */
    private fun setTestDate(date: String) {
        MutableTestClockConfig.instant.set(
            java.time.LocalDate.parse(date).atTime(15, 0).atZone(TimeUtil.SHANGHAI).toInstant(),
        )
    }

    private inline fun <reified T> call(
        method: HttpMethod,
        url: String,
        token: String? = null,
        body: Any? = null,
    ): ResponseEntity<T> {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            token?.let { setBearerAuth(it) }
        }
        return rest.exchange(url, method, HttpEntity(body, headers), T::class.java)
    }

    private fun login(user: String, pwd: String): String {
        val resp = call<LoginResponse>(HttpMethod.POST, "/api/auth/login", body = mapOf("loginName" to user, "password" to pwd))
        assertEquals(HttpStatus.OK, resp.statusCode, "登录 $user 失败: ${resp.body}")
        return resp.body!!.token
    }

    @Test
    fun `无 token 与伪造 token 均 401-1002`() {
        setTestDate("2026-09-05")
        val no = call<ErrorResponse>(HttpMethod.GET, "/api/me")
        assertEquals(HttpStatus.UNAUTHORIZED, no.statusCode)
        assertEquals(1002, no.body!!.code)

        val bad = call<ErrorResponse>(HttpMethod.GET, "/api/me", token = "not-a-jwt")
        assertEquals(HttpStatus.UNAUTHORIZED, bad.statusCode)
        assertEquals(1002, bad.body!!.code)
    }

    @Test
    fun `USER 访问 ADMIN 接口 - 403-1003`() {
        setTestDate("2026-09-05")
        val token = login("zhangsan", "123456")
        val resp = call<ErrorResponse>(HttpMethod.GET, "/api/admin/users", token = token)
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
        assertEquals(1003, resp.body!!.code)
    }

    @Test
    fun `全员可查当日点餐 - 普通用户 200（D-008）`() {
        setTestDate("2026-09-06")
        val user = login("zhangsan", "123456")
        call<Void>(HttpMethod.PUT, "/api/orders/today", token = user, body = mapOf("spicy" to true))

        val resp = call<Map<*, *>>(HttpMethod.GET, "/api/orders/today/all", token = user)
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals(1, (resp.body!!["total"] as Number).toInt())
        val orders = resp.body!!["orders"] as List<*>
        assertEquals(1, orders.size)
    }

    @Test
    fun `登录并查看个人信息`() {
        setTestDate("2026-09-05")
        val token = login("admin", "admin123")
        val me = call<UserView>(HttpMethod.GET, "/api/me", token = token)
        assertEquals(HttpStatus.OK, me.statusCode)
        assertEquals("管理员", me.body!!.displayName)
        assertEquals("ADMIN", me.body!!.role)
    }

    @Test
    fun `点餐全流程 - 登记 修改 查询 统计 取消`() {
        // 用 sunqi：其他测试用 zhangsan/lisi/wangwu/admin，避免共享数据目录相互干扰
        setTestDate("2026-09-07")
        val user = login("sunqi", "123456")
        val admin = login("admin", "admin123")

        // 清理同日可能存在的记录（测试方法顺序随机、共享数据目录）
        call<Void>(HttpMethod.DELETE, "/api/orders/today", token = user)

        val put1 = call<Map<*, *>>(HttpMethod.PUT, "/api/orders/today", token = user, body = mapOf("spicy" to true))
        assertEquals(HttpStatus.OK, put1.statusCode)
        assertEquals(false, put1.body!!["updated"])

        val put2 = call<Map<*, *>>(HttpMethod.PUT, "/api/orders/today", token = user, body = mapOf("spicy" to false))
        assertEquals(true, put2.body!!["updated"])

        val today = call<TodayStatusResponse>(HttpMethod.GET, "/api/orders/today", token = user)
        assertEquals(false, today.body!!.myOrder?.spicy)
        assertTrue(today.body!!.window.open)

        val all = call<Map<*, *>>(HttpMethod.GET, "/api/orders/today/all", token = admin)
        assertEquals(1, (all.body!!["total"] as Number).toInt())
        assertEquals(0, (all.body!!["spicy"] as Number).toInt())
        assertEquals(1, (all.body!!["nonSpicy"] as Number).toInt())

        val del = call<Void>(HttpMethod.DELETE, "/api/orders/today", token = user)
        assertEquals(HttpStatus.NO_CONTENT, del.statusCode)
        val delAgain = call<ErrorResponse>(HttpMethod.DELETE, "/api/orders/today", token = user)
        assertEquals(HttpStatus.NOT_FOUND, delAgain.statusCode)
        assertEquals(2002, delAgain.body!!.code)
    }

    @Test
    fun `改姓名与改密码 - 成功后旧 token 失效`() {
        setTestDate("2026-09-08")
        val token = login("wangwu", "123456")

        val profile = call<UserView>(HttpMethod.PUT, "/api/me/profile", token = token, body = mapOf("displayName" to "王五二"))
        assertEquals("王五二", profile.body!!.displayName)

        assertEquals(1004, call<ErrorResponse>(HttpMethod.PUT, "/api/me/profile", token = token, body = mapOf("displayName" to "")).body!!.code)

        // 旧密码错误（401 + 带 body 的 PUT 在 JDK HttpURLConnection 下会抛 HttpRetryException，
        // 该分支已由 AuthServiceTest 单测覆盖，集成层不再直测）
        // 新密码过短 → 400/1004
        val short = call<ErrorResponse>(HttpMethod.PUT, "/api/me/password", token = token, body = mapOf("oldPassword" to "123456", "newPassword" to "123"))
        assertEquals(1004, short.body!!.code)

        val ok = call<Map<*, *>>(HttpMethod.PUT, "/api/me/password", token = token, body = mapOf("oldPassword" to "123456", "newPassword" to "abcdef88"))
        assertEquals(HttpStatus.OK, ok.statusCode)
        val newToken = ok.body!!["token"] as String

        assertEquals(HttpStatus.UNAUTHORIZED, call<ErrorResponse>(HttpMethod.GET, "/api/me", token = token).statusCode)
        assertEquals(HttpStatus.OK, call<UserView>(HttpMethod.GET, "/api/me", token = newToken).statusCode)
    }

    @Test
    fun `区间统计与参数校验`() {
        setTestDate("2026-09-10")
        val admin = login("admin", "admin123")
        val zhang = login("zhangsan", "123456")
        val li = login("lisi", "123456")
        call<Void>(HttpMethod.PUT, "/api/orders/today", token = zhang, body = mapOf("spicy" to true))
        call<Void>(HttpMethod.PUT, "/api/orders/today", token = li, body = mapOf("spicy" to false))

        val summary = call<Map<*, *>>(HttpMethod.GET, "/api/admin/summary?from=2026-09-10&to=2026-09-10", token = admin)
        assertEquals(HttpStatus.OK, summary.statusCode)
        val daily = summary.body!!["daily"] as List<*>
        assertEquals(1, daily.size)
        assertEquals(2, ((daily[0] as Map<*, *>)["count"] as Number).toInt())
        // 种子 6 人起；若“管理员创建用户”用例先执行则会有第 7 人（测试顺序随机、共享数据目录）
        assertTrue((summary.body!!["perUser"] as List<*>).size >= 6)

        val reversed = call<ErrorResponse>(HttpMethod.GET, "/api/admin/summary?from=2026-09-07&to=2026-09-01", token = admin)
        assertEquals(1004, reversed.body!!.code)
        val badDate = call<ErrorResponse>(HttpMethod.GET, "/api/admin/summary?from=abc&to=2026-09-01", token = admin)
        assertEquals(1004, badDate.body!!.code)
    }

    @Test
    fun `导出 xlsx 可被 POI 读回 - 两个 Sheet`() {
        setTestDate("2026-09-09")
        val admin = login("admin", "admin123")
        val resp = call<ByteArray>(HttpMethod.GET, "/api/admin/export?from=2026-09-09&to=2026-09-09", token = admin)
        assertEquals(HttpStatus.OK, resp.statusCode)
        val disposition = resp.headers.getFirst("Content-Disposition")!!
        assertTrue(disposition.contains("filename*"))
        XSSFWorkbook(ByteArrayInputStream(resp.body!!)).use { wb ->
            assertEquals(2, wb.numberOfSheets)
            assertEquals("点餐明细", wb.getSheetName(0))
            assertEquals("人员汇总", wb.getSheetName(1))
        }
    }

    @Test
    fun `管理员创建用户 - 成功 重复 1005 非法 1004`() {
        setTestDate("2026-09-11")
        val admin = login("admin", "admin123")
        val created = call<UserView>(HttpMethod.POST, "/api/admin/users", token = admin, body = mapOf("loginName" to "zhouba", "displayName" to "周八", "password" to "123456"))
        assertEquals(HttpStatus.CREATED, created.statusCode)
        assertEquals("周八", created.body!!.displayName)

        val dup = call<ErrorResponse>(HttpMethod.POST, "/api/admin/users", token = admin, body = mapOf("loginName" to "zhouba", "displayName" to "周八二", "password" to "123456"))
        assertEquals(HttpStatus.CONFLICT, dup.statusCode)
        assertEquals(1005, dup.body!!.code)

        val invalid = call<ErrorResponse>(HttpMethod.POST, "/api/admin/users", token = admin, body = mapOf("loginName" to "x", "displayName" to "短", "password" to "123456"))
        assertEquals(1004, invalid.body!!.code)

        val list = call<Map<*, *>>(HttpMethod.GET, "/api/admin/users", token = admin)
        val names = (list.body!!["users"] as List<*>).map { (it as Map<*, *>)["loginName"] }
        assertTrue(names.contains("zhouba"))
    }

    @Test
    fun `SPA 回退 - 前端路由 200，未知 API 404`() {
        setTestDate("2026-09-12")
        val admin = login("admin", "admin123")

        val profile = rest.getForEntity("/profile", String::class.java)
        assertEquals(HttpStatus.OK, profile.statusCode)
        assertTrue(profile.body!!.contains("WEB-SHELL"))

        val history = rest.getForEntity("/admin/history", String::class.java)
        assertEquals(HttpStatus.OK, history.statusCode)
        assertTrue(history.body!!.contains("ADMIN-SHELL"))

        val none = call<ErrorResponse>(HttpMethod.GET, "/api/none", token = admin)
        assertEquals(HttpStatus.NOT_FOUND, none.statusCode)
        assertEquals(404, none.body!!.code)
    }

    @Test
    fun `jwt 服务可独立校验`() {
        val props = LunchProperties(jwtSecret = "0123456789abcdef0123456789abcdef")
        val jwt = JwtService(props)
        val token = jwt.create("abc", "USER", 3)
        val info = jwt.parse(token)!!
        assertEquals(3, info.pwdVer)
        assertFalse(jwt.parse(token + "x") != null)
    }

    @Test
    fun `管理员设置点餐窗口 - 保存与校验（D-009）`() {
        setTestDate("2026-09-15")
        val admin = login("admin", "admin123")
        // 放宽窗口（仍包含固定测试时刻 15:00，不影响其他用例）
        val put = call<Map<*, *>>(HttpMethod.PUT, "/api/admin/settings/window", token = admin, body = mapOf("start" to "09:00", "end" to "23:00"))
        assertEquals(HttpStatus.OK, put.statusCode)
        assertEquals("09:00", put.body!!["orderWindowStart"])

        val get = call<Map<*, *>>(HttpMethod.GET, "/api/admin/settings/window", token = admin)
        assertEquals("09:00", get.body!!["orderWindowStart"])
        assertEquals("23:00", get.body!!["orderWindowEnd"])

        // start > end → 1004 且不落盘
        val bad = call<ErrorResponse>(HttpMethod.PUT, "/api/admin/settings/window", token = admin, body = mapOf("start" to "20:00", "end" to "08:00"))
        assertEquals(1004, bad.body!!.code)
        val after = call<Map<*, *>>(HttpMethod.GET, "/api/admin/settings/window", token = admin)
        assertEquals("09:00", after.body!!["orderWindowStart"])

        // 普通用户无权设置
        val user = login("zhangsan", "123456")
        assertEquals(
            HttpStatus.FORBIDDEN,
            call<ErrorResponse>(HttpMethod.PUT, "/api/admin/settings/window", token = user, body = mapOf("start" to "09:00", "end" to "23:00")).statusCode,
        )
    }

    @Test
    fun `批量创建用户（D-010）`() {
        setTestDate("2026-09-15")
        val admin = login("admin", "admin123")
        val resp = call<Map<*, *>>(
            HttpMethod.POST,
            "/api/admin/users/bulk",
            token = admin,
            body = mapOf(
                "users" to listOf(
                    mapOf("loginName" to "batch01", "displayName" to "批量一"),
                    mapOf("loginName" to "batch02", "displayName" to "批量二", "password" to "abc12345"),
                    mapOf("loginName" to "batch01", "displayName" to "批内重复"),
                    mapOf("loginName" to "zhangsan", "displayName" to "与现有重复"),
                    mapOf("loginName" to "bad name!", "displayName" to "非法登录名"),
                ),
            ),
        )
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals(2, (resp.body!!["created"] as Number).toInt())
        assertEquals(3, (resp.body!!["failed"] as List<*>).size)

        // 缺省密码与自定义密码均可登录
        assertEquals(HttpStatus.OK, call<UserView>(HttpMethod.GET, "/api/me", token = login("batch01", "123456")).statusCode)
        assertEquals(HttpStatus.OK, call<UserView>(HttpMethod.GET, "/api/me", token = login("batch02", "abc12345")).statusCode)
    }

    @Test
    fun `导出全部账户 xlsx 含可解密密码（D-010）`() {
        setTestDate("2026-09-15")
        val admin = login("admin", "admin123")
        val resp = call<ByteArray>(HttpMethod.GET, "/api/admin/users/export", token = admin)
        assertEquals(HttpStatus.OK, resp.statusCode)
        XSSFWorkbook(ByteArrayInputStream(resp.body!!)).use { wb ->
            assertEquals("账户列表", wb.getSheetName(0))
            val sheet = wb.getSheetAt(0)
            // 至少包含种子 6 人
            assertTrue(sheet.lastRowNum >= 6)
            var found = false
            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                if (row.getCell(0).stringCellValue == "zhangsan") {
                    // 种子账号的密码可逆导出为明文
                    assertEquals("123456", row.getCell(3).stringCellValue)
                    found = true
                }
            }
            assertTrue(found)
        }
        // 普通用户无权导出
        val user = login("zhangsan", "123456")
        assertEquals(HttpStatus.FORBIDDEN, call<ErrorResponse>(HttpMethod.GET, "/api/admin/users/export", token = user).statusCode)
    }

    @Test
    fun `公共聊天频道 - 发送 当天查询 管理员查历史（D-013）`() {
        setTestDate("2026-09-18")
        val user = login("sunqi", "123456")
        val admin = login("admin", "admin123")

        val put = call<Map<*, *>>(HttpMethod.PUT, "/api/orders/today/chat", token = user, body = mapOf("content" to "今天吃什么"))
        assertEquals(HttpStatus.OK, put.statusCode)
        assertEquals("今天吃什么", put.body!!["content"])

        // 空内容与超长 → 1004
        assertEquals(1004, call<ErrorResponse>(HttpMethod.PUT, "/api/orders/today/chat", token = user, body = mapOf("content" to "   ")).body!!.code)
        assertEquals(1004, call<ErrorResponse>(HttpMethod.PUT, "/api/orders/today/chat", token = user, body = mapOf("content" to "长".repeat(201))).body!!.code)

        // 多人聊天，当天查询
        call<Map<*, *>>(HttpMethod.PUT, "/api/orders/today/chat", token = admin, body = mapOf("content" to "吃吧"))
        val today = call<Map<*, *>>(HttpMethod.GET, "/api/orders/today/chat", token = user)
        assertEquals("2026-09-18", today.body!!["date"])
        assertEquals(2, (today.body!!["count"] as Number).toInt())

        // 客户端只能看当天：时钟切到次日即为空
        setTestDate("2026-09-19")
        val nextDay = call<Map<*, *>>(HttpMethod.GET, "/api/orders/today/chat", token = user)
        assertEquals(0, (nextDay.body!!["count"] as Number).toInt())

        // 管理员仍可查历史日期；普通用户无权查管理端历史
        val history = call<Map<*, *>>(HttpMethod.GET, "/api/admin/chat?date=2026-09-18", token = admin)
        assertEquals(2, (history.body!!["count"] as Number).toInt())
        assertEquals(HttpStatus.FORBIDDEN, call<ErrorResponse>(HttpMethod.GET, "/api/admin/chat?date=2026-09-18", token = user).statusCode)
    }

    @Test
    fun `定时通知设置 - 管理员修改 客户端同步（D-014）`() {
        setTestDate("2026-09-18")
        val admin = login("admin", "admin123")
        val user = login("zhangsan", "123456")

        val put = call<Map<*, *>>(
            HttpMethod.PUT,
            "/api/admin/settings/notify",
            token = admin,
            body = mapOf("notifyTime" to "11:30", "notifyTitle" to "开饭啦", "notifyContent" to "快去点餐"),
        )
        assertEquals(HttpStatus.OK, put.statusCode)
        assertEquals("11:30", put.body!!["notifyTime"])

        // 客户端同步端点（Android 拉取后本地调度）
        val sync = call<Map<*, *>>(HttpMethod.GET, "/api/settings/notify", token = user)
        assertEquals("11:30", sync.body!!["notifyTime"])
        assertEquals("开饭啦", sync.body!!["notifyTitle"])

        // 非法时间 / 标题超长 → 1004
        assertEquals(1004, call<ErrorResponse>(HttpMethod.PUT, "/api/admin/settings/notify", token = admin, body = mapOf("notifyTime" to "abc", "notifyTitle" to "t", "notifyContent" to "c")).body!!.code)
        assertEquals(1004, call<ErrorResponse>(HttpMethod.PUT, "/api/admin/settings/notify", token = admin, body = mapOf("notifyTime" to "11:30", "notifyTitle" to "长".repeat(31), "notifyContent" to "c")).body!!.code)

        // 普通用户无权修改
        assertEquals(
            HttpStatus.FORBIDDEN,
            call<ErrorResponse>(HttpMethod.PUT, "/api/admin/settings/notify", token = user, body = mapOf("notifyTime" to "11:30", "notifyTitle" to "t", "notifyContent" to "c")).statusCode,
        )

        // 恢复默认
        call<Map<*, *>>(HttpMethod.PUT, "/api/admin/settings/notify", token = admin, body = mapOf("notifyTime" to "17:30", "notifyTitle" to "午餐点餐提醒", "notifyContent" to "今天需要点餐吗？请在 18:00 前登记"))
    }

    @Test
    fun `我的历史点餐（D-011）`() {
        setTestDate("2026-09-17")
        val user = login("sunqi", "123456")
        call<Void>(HttpMethod.PUT, "/api/orders/today", token = user, body = mapOf("spicy" to false))

        val resp = call<Map<*, *>>(HttpMethod.GET, "/api/me/orders?from=2026-09-17&to=2026-09-17", token = user)
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals(1, (resp.body!!["totalDays"] as Number).toInt())
        assertEquals(1, (resp.body!!["nonSpicyDays"] as Number).toInt())
        val records = resp.body!!["records"] as List<*>
        assertEquals(1, records.size)
        assertEquals("2026-09-17", (records[0] as Map<*, *>)["date"])

        // 范围非法 → 1004
        assertEquals(1004, call<ErrorResponse>(HttpMethod.GET, "/api/me/orders?from=2026-09-17&to=2026-09-01", token = user).body!!.code)
    }
}
