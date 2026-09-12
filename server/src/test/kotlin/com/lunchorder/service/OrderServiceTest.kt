package com.lunchorder.service

import com.lunchorder.LunchProperties
import com.lunchorder.exception.BusinessException
import com.lunchorder.store.DataStore
import com.lunchorder.util.TimeUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class OrderServiceTest {

    @TempDir
    lateinit var temp: Path

    private lateinit var store: DataStore
    private lateinit var service: OrderService
    private lateinit var closedWindowService: OrderService

    /** 2026-09-07 15:00（+08:00，窗口内，周一） */
    private val clock15: Clock = Clock.fixed(Instant.parse("2026-09-07T07:00:00Z"), TimeUtil.SHANGHAI)
    /** 同日 12:00（窗口外） */
    private val clock12: Clock = Clock.fixed(Instant.parse("2026-09-07T04:00:00Z"), TimeUtil.SHANGHAI)

    private val date: LocalDate = LocalDate.of(2026, 9, 7)

    @BeforeEach
    fun setup() {
        val dir = temp.resolve("o-${UUID.randomUUID()}").toString()
        store = DataStore(LunchProperties(dataDir = dir))
        service = OrderService(store, WindowService(store, LunchProperties(dataDir = dir)), clock15)
        closedWindowService = OrderService(
            store,
            WindowService(store, LunchProperties(dataDir = dir, windowStart = "02:00", windowEnd = "03:00")),
            clock12,
        )
    }

    private fun user(name: String) = store.readUsers().users.first { it.loginName == name }

    @Test
    fun `首次登记 updated=false 重复提交视为修改`() {
        val v1 = service.upsert(user("zhangsan"), true, ZonedDateTime.now(clock15))
        assertFalse(v1.updated)
        assertTrue(v1.spicy)

        val v2 = service.upsert(user("zhangsan"), false, ZonedDateTime.now(clock15))
        assertTrue(v2.updated)
        assertFalse(v2.spicy)

        val day = store.readDay(date)!!
        assertEquals(1, day.orders.size)
        assertEquals("张三", day.orders[0].displayName)
    }

    @Test
    fun `取消后再取消 - 2002`() {
        service.upsert(user("zhangsan"), true, ZonedDateTime.now(clock15))
        service.cancel("zhangsan", ZonedDateTime.now(clock15), isAdmin = false)
        assertEquals(0, store.readDay(date)!!.orders.size)
        assertEquals(2002, assertThrows<BusinessException> {
            service.cancel("zhangsan", ZonedDateTime.now(clock15), isAdmin = false)
        }.code)
    }

    @Test
    fun `窗口外普通用户拒绝 - 2001，管理员不受限`() {
        assertEquals(2001, assertThrows<BusinessException> {
            closedWindowService.upsert(user("zhangsan"), true, ZonedDateTime.now(clock12))
        }.code)
        assertEquals(2001, assertThrows<BusinessException> {
            closedWindowService.cancel("zhangsan", ZonedDateTime.now(clock12), isAdmin = false)
        }.code)
        val adminOrder = closedWindowService.upsert(user("admin"), true, ZonedDateTime.now(clock12))
        assertFalse(adminOrder.updated)
    }

    @Test
    fun `todayStatus - 无记录 myOrder 为 null，登记后回显`() {
        val before = service.todayStatus("zhangsan", ZonedDateTime.now(clock15))
        assertNull(before.myOrder)
        assertTrue(before.window.open)
        assertEquals("2026-09-07", before.date)

        service.upsert(user("zhangsan"), true, ZonedDateTime.now(clock15))
        val after = service.todayStatus("zhangsan", ZonedDateTime.now(clock15))
        assertEquals(true, after.myOrder?.spicy)
    }
}
