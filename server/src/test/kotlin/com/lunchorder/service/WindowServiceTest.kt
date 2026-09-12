package com.lunchorder.service

import com.lunchorder.LunchProperties
import com.lunchorder.domain.AppConfigJson
import com.lunchorder.store.DataStore
import com.lunchorder.util.TimeUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.ZonedDateTime
import java.util.UUID

class WindowServiceTest {

    @TempDir
    lateinit var temp: Path

    private fun service(windowStart: String = "", windowEnd: String = "", config: AppConfigJson? = null): WindowService {
        val props = LunchProperties(
            dataDir = temp.resolve("w-${UUID.randomUUID()}").toString(),
            windowStart = windowStart,
            windowEnd = windowEnd,
        )
        val store = DataStore(props)
        config?.let { store.writeConfig(it) }
        return WindowService(store, props)
    }

    private fun at(hour: Int, minute: Int, second: Int = 0): ZonedDateTime =
        ZonedDateTime.of(2026, 9, 5, hour, minute, second, 0, TimeUtil.SHANGHAI)

    @Test
    fun `窗口边界 - 13时59分59秒 拒绝`() {
        assertFalse(service().isOpen(at(13, 59, 59)))
    }

    @Test
    fun `窗口边界 - 14时00分00秒 通过`() {
        assertTrue(service().isOpen(at(14, 0, 0)))
    }

    @Test
    fun `窗口边界 - 17时59分59秒 通过`() {
        assertTrue(service().isOpen(at(17, 59, 59)))
    }

    @Test
    fun `窗口边界 - 18时00分00秒 通过（闭区间）`() {
        assertTrue(service().isOpen(at(18, 0, 0)))
    }

    @Test
    fun `窗口边界 - 18时00分01秒 拒绝`() {
        assertFalse(service().isOpen(at(18, 0, 1)))
    }

    @Test
    fun `环境变量覆盖窗口`() {
        assertTrue(service(windowStart = "09:00", windowEnd = "10:00").isOpen(at(9, 30)))
        assertFalse(service(windowStart = "09:00", windowEnd = "10:00").isOpen(at(10, 1)))
    }

    @Test
    fun `config json 覆盖窗口`() {
        assertTrue(service(config = AppConfigJson("09:00", "10:00")).isOpen(at(9, 30)))
    }

    @Test
    fun `windowInfo 字段完整`() {
        val info = service().windowInfo(at(15, 0))
        assertEquals("14:00", info.start)
        assertEquals("18:00", info.end)
        assertTrue(info.open)
        assertTrue(info.serverTime.contains("+08:00"))
    }
}
