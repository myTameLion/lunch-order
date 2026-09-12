package com.lunchorder.store

import com.lunchorder.LunchProperties
import com.lunchorder.domain.DayOrders
import com.lunchorder.domain.OrderRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

class DataStoreTest {

    @TempDir
    lateinit var temp: Path

    private fun newStore(dir: Path): DataStore =
        DataStore(LunchProperties(dataDir = dir.toString(), jwtSecret = "0123456789abcdef0123456789abcdef"))

    @Test
    fun `首次启动种子初始化`() {
        val dir = temp.resolve("a")
        val store = newStore(dir)
        assertTrue(Files.exists(dir.resolve("users.json")))
        assertTrue(Files.exists(dir.resolve("config.json")))
        assertTrue(Files.exists(dir.resolve("orders")))

        val users = store.readUsers().users
        assertEquals(6, users.size)
        val admin = users.first { it.loginName == "admin" }
        assertEquals("ADMIN", admin.role)
        assertEquals("管理员", admin.displayName)
        assertTrue(admin.passwordHash.startsWith("\$2"))
        assertEquals(6, users.size)
        assertEquals(5, users.count { it.role == "USER" })
    }

    @Test
    fun `原子写 - 反复覆写后文件仍可完整解析`() {
        val store = newStore(temp.resolve("b"))
        val date = LocalDate.of(2026, 9, 5)
        repeat(20) { i ->
            store.writeDay(DayOrders(date.toString(), mutableListOf(OrderRecord("u$i", "名$i", i % 2 == 0, "2026-09-05T14:23:05+08:00"))))
        }
        val day = store.readDay(date)
        assertNotNull(day)
        assertEquals(1, day!!.orders.size)
        assertEquals("u19", day.orders[0].loginName)
    }

    @Test
    fun `点餐文件缺失按空处理`() {
        val store = newStore(temp.resolve("c"))
        assertNull(store.readDay(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun `users json 损坏 - 启动时备份并重新种子`() {
        val dir = temp.resolve("d")
        Files.createDirectories(dir)
        Files.writeString(dir.resolve("users.json"), "{not json")
        val store = newStore(dir)
        assertEquals(6, store.readUsers().users.size)
        val hasBackup = Files.list(dir).use { stream ->
            stream.anyMatch { p -> p.fileName.toString().startsWith("users.json.corrupt") }
        }
        assertTrue(hasBackup)
    }

    @Test
    fun `orders 文件损坏 - 读失败不崩溃且按空处理`() {
        val store = newStore(temp.resolve("e"))
        val file = temp.resolve("e").resolve("orders").resolve("2026-09-05.json")
        Files.writeString(file, "garbage")
        assertNull(store.readDay(LocalDate.of(2026, 9, 5)))
    }

    @Test
    fun `运行中 users json 损坏 - readUsersSafe 按空表处理`() {
        val store = newStore(temp.resolve("f"))
        Files.writeString(temp.resolve("f").resolve("users.json"), "garbage-after-init")
        assertEquals(0, store.readUsersSafe().users.size)
        assertFalse(store.readUsersSafe().users.isNotEmpty())
    }
}
