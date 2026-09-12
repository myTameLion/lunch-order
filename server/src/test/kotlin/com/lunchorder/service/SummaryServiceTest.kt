package com.lunchorder.service

import com.lunchorder.LunchProperties
import com.lunchorder.domain.DayOrders
import com.lunchorder.domain.OrderRecord
import com.lunchorder.exception.BusinessException
import com.lunchorder.store.DataStore
import com.lunchorder.util.TimeUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class SummaryServiceTest {

    @TempDir
    lateinit var temp: Path

    private lateinit var store: DataStore
    private lateinit var summary: SummaryService

    /** 2026-09-07 15:00（+08:00，窗口内，周一） */
    private val clock15: Clock = Clock.fixed(Instant.parse("2026-09-07T07:00:00Z"), TimeUtil.SHANGHAI)

    @BeforeEach
    fun setup() {
        val dir = temp.resolve("s-${UUID.randomUUID()}").toString()
        store = DataStore(LunchProperties(dataDir = dir))
        summary = SummaryService(store, WindowService(store, LunchProperties(dataDir = dir)))
    }

    private fun putDay(date: String, orders: List<Triple<String, Boolean, String>>) {
        store.writeDay(
            DayOrders(date, orders.map { OrderRecord(it.first, it.first, it.second, it.third) }.toMutableList()),
        )
    }

    @Test
    fun `区间统计 - 逐日含 0 值天，perUser 含 0 单用户且排序正确`() {
        putDay(
            "2026-09-01",
            listOf(
                Triple("zhangsan", true, "2026-09-01T14:10:00+08:00"),
                Triple("lisi", false, "2026-09-01T15:10:00+08:00"),
            ),
        )
        putDay("2026-09-03", listOf(Triple("zhangsan", false, "2026-09-03T14:10:00+08:00")))

        val r = summary.rangeSummary(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-03"))

        assertEquals(3, r.daily.size)
        assertEquals(2, r.daily[0].count)
        assertEquals(0, r.daily[1].count)
        assertEquals(1, r.daily[2].count)
        assertEquals(3, r.totalOrders)
        assertEquals(1, r.totalSpicy)
        assertEquals(2, r.totalNonSpicy)

        assertEquals(6, r.perUser.size)
        val zs = r.perUser.first { it.loginName == "zhangsan" }
        assertEquals(2, zs.days)
        assertEquals(1, zs.spicyDays)
        assertEquals(1, zs.nonSpicyDays)
        assertEquals(1, r.perUser.first { it.loginName == "lisi" }.days)
        assertEquals(0, r.perUser.first { it.loginName == "admin" }.days)
        // days 降序、loginName 升序
        assertEquals("zhangsan", r.perUser[0].loginName)
        assertTrue(r.perUser.zipWithNext().all { (a, b) -> a.days >= b.days })
    }

    @Test
    fun `日期范围非法 - 1004`() {
        assertEquals(1004, assertThrows<BusinessException> {
            summary.rangeSummary(LocalDate.parse("2026-09-05"), LocalDate.parse("2026-09-01"))
        }.code)
        assertEquals(1004, assertThrows<BusinessException> {
            summary.rangeSummary(LocalDate.parse("2026-01-01"), LocalDate.parse("2027-03-01"))
        }.code)
    }

    @Test
    fun `todayAll 统计口径`() {
        putDay(
            "2026-09-07",
            listOf(
                Triple("zhangsan", true, "2026-09-07T14:10:00+08:00"),
                Triple("lisi", false, "2026-09-07T14:20:00+08:00"),
                Triple("wangwu", true, "2026-09-07T14:30:00+08:00"),
            ),
        )
        val all = summary.todayAll(ZonedDateTime.now(clock15))
        assertEquals(3, all.total)
        assertEquals(2, all.spicy)
        assertEquals(1, all.nonSpicy)
        assertEquals(3, all.orders.size)
        assertTrue(all.window.open)
    }

    @Test
    fun `导出 xlsx - 两个 Sheet 名称与行数正确且可被 POI 读回`() {
        putDay(
            "2026-09-01",
            listOf(
                Triple("zhangsan", true, "2026-09-01T14:10:00+08:00"),
                Triple("lisi", false, "2026-09-01T15:10:00+08:00"),
            ),
        )
        val bytes = summary.exportXlsx(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-02"))
        XSSFWorkbook(ByteArrayInputStream(bytes)).use { wb ->
            assertEquals(2, wb.numberOfSheets)
            assertEquals("点餐明细", wb.getSheetName(0))
            assertEquals("人员汇总", wb.getSheetName(1))
            // 表头 + 2 条明细
            assertEquals(2, wb.getSheetAt(0).lastRowNum)
            val detail = wb.getSheetAt(0).getRow(1)
            assertEquals("2026-09-01", detail.getCell(0).stringCellValue)
            assertEquals("是", detail.getCell(3).stringCellValue)
            // 人员汇总：表头 + 全部 6 个用户
            assertEquals(6, wb.getSheetAt(1).lastRowNum)
        }
    }
}
