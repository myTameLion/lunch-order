package com.intranet.lunchorder

import com.intranet.lunchorder.data.api.ApiException
import com.intranet.lunchorder.data.api.ApiFactory
import com.intranet.lunchorder.data.api.ApiService
import com.intranet.lunchorder.data.api.AuthEvents
import com.intranet.lunchorder.data.api.AuthInterceptor
import com.intranet.lunchorder.data.api.ErrorParser
import com.intranet.lunchorder.data.repo.LunchRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

/**
 * API 层与仓库层集成测试：MockWebServer + 真实 Retrofit/序列化链路（契约 api.yaml v1.0.0）。
 */
class ApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: LunchRepository
    private val prefs = FakePrefsStore(initialToken = "token-abc")

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(prefs)).build())
            .addConverterFactory(ApiFactory.json.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
            .build()
            .create(ApiService::class.java)
        repo = LunchRepository(api, prefs)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `登录成功 - 解析 token 并不携带 Bearer`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"token":"tk1","loginName":"zhangsan","displayName":"张三","role":"USER"}"""),
        )
        val resp = repo.login("zhangsan", "123456")
        assertEquals("tk1", resp.token)
        assertEquals("张三", resp.displayName)
        assertEquals("USER", resp.role)

        val recorded = server.takeRequest()
        assertEquals("/api/auth/login", recorded.path)
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun `登录失败 401 - 1001 不触发全局登出`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"code":1001,"message":"登录名或密码错误"}"""))
        val e = runCatching { repo.login("zhangsan", "bad") }.exceptionOrNull()
        assertTrue(e is ApiException)
        assertEquals(1001, (e as ApiException).code)
        assertEquals(false, prefs.clearedSession)
    }

    @Test
    fun `带凭证请求自动附加 Bearer`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"date":"2026-09-07","window":{"start":"14:00","end":"18:00","open":true,"serverTime":"2026-09-07T15:00:00+08:00"},"myOrder":null}"""),
        )
        repo.getToday()
        assertEquals("Bearer token-abc", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `getToday - myOrder 为 null 与非 null 两种解析`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"date":"2026-09-07","window":{"start":"14:00","end":"18:00","open":true,"serverTime":"2026-09-07T15:00:00+08:00"},"myOrder":null}"""),
        )
        assertNull(repo.getToday().myOrder)

        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"date":"2026-09-07","window":{"start":"14:00","end":"18:00","open":false,"serverTime":"2026-09-07T19:00:00+08:00"},"myOrder":{"spicy":true,"orderedAt":"2026-09-07T14:23:05+08:00"}}"""),
        )
        val status = repo.getToday()
        assertEquals(true, status.myOrder?.spicy)
        assertEquals(false, status.window.open)
    }

    @Test
    fun `getTodayAll - 全员当日统计解析（D-008）`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"date":"2026-09-07","total":2,"spicy":1,"nonSpicy":1,"orders":[
                    {"loginName":"zhangsan","displayName":"张三","spicy":true,"orderedAt":"2026-09-07T14:23:05+08:00"},
                    {"loginName":"lisi","displayName":"李四","spicy":false,"orderedAt":"2026-09-07T15:07:41+08:00"}]}"""),
        )
        val all = repo.getTodayAll()
        assertEquals(2, all.total)
        assertEquals(1, all.spicy)
        assertEquals(1, all.nonSpicy)
        assertEquals(2, all.orders.size)
        assertEquals("张三", all.orders[0].displayName)
        assertTrue(all.orders[0].spicy)
    }

    @Test
    fun `我的历史点餐解析（D-011）`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"from":"2026-09-01","to":"2026-09-07","records":[
                    {"date":"2026-09-05","spicy":true,"orderedAt":"2026-09-05T14:23:05+08:00"}],
                    "totalDays":1,"spicyDays":1,"nonSpicyDays":0}"""),
        )
        val resp = repo.getMyOrders("2026-09-01", "2026-09-07")
        assertEquals(1, resp.totalDays)
        assertEquals(1, resp.spicyDays)
        assertEquals("2026-09-05", resp.records[0].date)
        val path = server.takeRequest().path.orEmpty()
        assertTrue(path.startsWith("/api/me/orders"))
        assertTrue(path.contains("from=2026-09-01"))
        assertTrue(path.contains("to=2026-09-07"))
    }

    @Test
    fun `聊天频道 - 发送与当天查询（D-013）`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"loginName":"zhangsan","displayName":"张三","content":"今天吃什么","sentAt":"2026-09-11T15:01:00+08:00"}"""),
        )
        val sent = repo.sendChat("今天吃什么")
        assertEquals("今天吃什么", sent.content)
        assertEquals("/api/orders/today/chat", server.takeRequest().path)

        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"date":"2026-09-11","count":1,"messages":[
                    {"loginName":"zhangsan","displayName":"张三","content":"今天吃什么","sentAt":"2026-09-11T15:01:00+08:00"}]}"""),
        )
        val chat = repo.getTodayChat()
        assertEquals(1, chat.count)
        assertEquals("张三", chat.messages[0].displayName)
    }

    @Test
    fun `通知设置同步（D-014）`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"notifyTime":"11:30","notifyTitle":"开饭啦","notifyContent":"快去点餐"}"""),
        )
        val n = repo.getNotifySettings()
        assertEquals("11:30", n.notifyTime)
        assertEquals("开饭啦", n.notifyTitle)
        assertEquals("/api/settings/notify", server.takeRequest().path)
    }

    @Test
    fun `重要通知解析（D-015，最新在前）`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"notices":[
                    {"id":2,"title":"新功能","content":"聊天频道上线啦","createdAt":"2026-09-11T10:00:00+08:00","createdBy":"admin"},
                    {"id":1,"title":"系统维护","content":"周日停机","createdAt":"2026-09-10T10:00:00+08:00","createdBy":"admin"}]}"""),
        )
        val resp = repo.getNotices()
        assertEquals(2, resp.notices.size)
        assertEquals("新功能", resp.notices[0].title)
        assertEquals("/api/notices", server.takeRequest().path)
    }

    @Test
    fun `putOrder 成功解析 updated`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"loginName":"zhangsan","displayName":"张三","spicy":true,"orderedAt":"2026-09-07T14:23:05+08:00","updated":false}"""),
        )
        val view = repo.putOrder(true)
        assertEquals(false, view.updated)
        assertEquals(true, view.spicy)

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"spicy\":true"))
    }

    @Test
    fun `取消点餐 204 成功，404 - 2002`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        repo.cancelOrder()
        assertEquals("DELETE", server.takeRequest().method)

        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"code":2002,"message":"今天还没有你的点餐记录"}"""))
        val e = runCatching { repo.cancelOrder() }.exceptionOrNull()
        assertEquals(2002, (e as ApiException).code)
    }

    @Test
    fun `业务 2001 错误体解析`() = runTest {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"code":2001,"message":"当前不在点餐时间（14:00 - 18:00）"}"""))
        val e = runCatching { repo.putOrder(true) }.exceptionOrNull()
        assertEquals(2001, (e as ApiException).code)
        assertTrue((e as ApiException).message.contains("点餐时间"))
    }

    @Test
    fun `带凭证请求 401 - 清会话并发全局登出事件`() = runTest {
        val events = mutableListOf<Unit>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            AuthEvents.logout.collect { events.add(Unit) }
        }
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"code":1002,"message":"登录状态已失效"}"""))
        runCatching { repo.getToday() }
        // SharedFlow 事件在测试调度器队列中，推进到静止态再断言
        testScheduler.advanceUntilIdle()
        assertTrue(prefs.clearedSession)
        assertEquals(1, events.size)
        collector.cancel()
    }

    @Test
    fun `修改密码成功后保存新 token（D-004）`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"token":"new-token"}"""))
        val token = repo.changePassword("123456", "abcdef88")
        assertEquals("new-token", token)
        assertEquals("new-token", prefs.token)
    }
}

class ErrorParserTest {

    @Test
    fun `解析契约错误体`() {
        val e = ErrorParser.parse(409, """{"code":2001,"message":"当前不在点餐时间"}""")
        assertEquals(2001, e.code)
        assertEquals("当前不在点餐时间", e.message)
    }

    @Test
    fun `非 JSON 错误体退化为 HTTP 状态码`() {
        val e = ErrorParser.parse(500, "<html>oops</html>")
        assertEquals(500, e.code)
        assertTrue(e.message.contains("500"))
    }

    @Test
    fun `空错误体退化为 HTTP 状态码`() {
        val e = ErrorParser.parse(404, null)
        assertEquals(404, e.code)
    }

    @Test
    fun `IOException 归一化为网络错误`() {
        val e = ErrorParser.fromThrowable(java.io.IOException("connection refused"))
        assertEquals(ApiException.CODE_NETWORK, e.code)
        assertTrue(e.isNetworkError)
    }

    @Test
    fun `未知异常归一化`() {
        val e = ErrorParser.fromThrowable(IllegalStateException("boom"))
        assertEquals(ApiException.CODE_UNKNOWN, e.code)
    }
}

class PrefsStoreTest {

    @Test
    fun `clearSession 清凭证但保留服务器与提醒设置`() {
        val prefs = FakePrefsStore(initialToken = "tk")
        prefs.loginName = "zhangsan"
        prefs.displayName = "张三"
        prefs.role = "USER"
        prefs.serverUrl = "http://10.0.0.5:8080/"
        prefs.notifyEnabled = true
        prefs.notifyTime = "11:30"

        prefs.clearSession()

        assertEquals("", prefs.token)
        assertEquals("", prefs.loginName)
        assertEquals("", prefs.role)
        assertEquals("http://10.0.0.5:8080/", prefs.serverUrl)
        assertEquals(true, prefs.notifyEnabled)
        assertEquals("11:30", prefs.notifyTime)
        assertTrue(prefs.clearedSession)
    }

    @Test
    fun `默认值符合契约`() {
        val prefs = FakePrefsStore()
        assertEquals("http://192.168.1.100:8080", prefs.serverUrl)
        assertEquals("17:30", prefs.notifyTime)
        assertEquals(false, prefs.notifyEnabled)
    }
}
