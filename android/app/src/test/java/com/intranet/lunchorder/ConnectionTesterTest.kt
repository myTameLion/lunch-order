package com.intranet.lunchorder

import com.intranet.lunchorder.data.api.ConnectionTester
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 连通性自检：任意 HTTP 响应即可达，IOException/非法地址不可达（登录页与设置页共用） */
class ConnectionTesterTest {

    @Test
    fun `收到 401 响应即判定可达`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"code":1002,"message":"登录状态已失效"}"""))
        server.start()
        try {
            val result = ConnectionTester.test(server.url("/").toString())
            assertTrue(result.ok)
            assertTrue(result.detail.contains("401"))
            assertEquals("/api/orders/today", server.takeRequest().path)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `200 同样可达`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        server.start()
        try {
            val result = ConnectionTester.test(server.url("/").toString())
            assertTrue(result.ok)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `连接被拒 - 判定不可达`() {
        // 端口 1 几乎必然无服务，连接立即被拒
        val result = ConnectionTester.test("http://127.0.0.1:1/")
        assertFalse(result.ok)
    }

    @Test
    fun `非法地址 - 不可达且不抛异常`() {
        val result = ConnectionTester.test("::::")
        assertFalse(result.ok)
    }
}
