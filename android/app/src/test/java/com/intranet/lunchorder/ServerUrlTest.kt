package com.intranet.lunchorder

import com.intranet.lunchorder.logic.ServerUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerUrlTest {

    @Test
    fun `无协议自动补 http`() {
        assertEquals("http://192.168.1.5:8080/", ServerUrl.normalize("192.168.1.5:8080"))
    }

    @Test
    fun `去空白与多余斜杠`() {
        assertEquals("http://a.example.com/", ServerUrl.normalize("  http://a.example.com///  "))
    }

    @Test
    fun `已规范地址保持不变`() {
        assertEquals("http://192.168.1.100:8080/", ServerUrl.normalize("http://192.168.1.100:8080"))
    }

    @Test
    fun `保留路径前缀`() {
        assertEquals("http://a.com:8080/lunch/", ServerUrl.normalize("http://a.com:8080/lunch/"))
    }

    @Test
    fun `todayEndpoint 拼接今日点餐路径`() {
        assertEquals(
            "http://192.168.1.100:8080/api/orders/today",
            ServerUrl.todayEndpoint("http://192.168.1.100:8080"),
        )
    }

    @Test
    fun `isValidBase 判定`() {
        assertTrue(ServerUrl.isValidBase("http://192.168.1.100:8080"))
        assertTrue(ServerUrl.isValidBase("192.168.1.100:8080"))
        assertFalse(ServerUrl.isValidBase("::::"))
        assertFalse(ServerUrl.isValidBase("not a url !@#"))
    }
}
