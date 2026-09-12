package com.lunchorder.service

import com.lunchorder.domain.DayOrders
import com.lunchorder.exception.BusinessException
import com.lunchorder.store.DataStore
import com.lunchorder.web.dto.DailySummary
import com.lunchorder.web.dto.RangeSummary
import com.lunchorder.web.dto.TodayAllResponse
import com.lunchorder.web.dto.UserSummary
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Service
class SummaryService(
    private val store: DataStore,
    private val windowService: WindowService,
) {

    fun todayAll(now: ZonedDateTime): TodayAllResponse {
        val date = now.toLocalDate()
        val orders = store.readDay(date)?.orders ?: mutableListOf()
        val spicy = orders.count { it.spicy }
        return TodayAllResponse(
            date = date.toString(),
            window = windowService.windowInfo(now),
            total = orders.size,
            spicy = spicy,
            nonSpicy = orders.size - spicy,
            orders = orders.toList(),
        )
    }

    fun rangeSummary(from: LocalDate, to: LocalDate): RangeSummary {
        requireRange(from, to)
        val dates = datesBetween(from, to)
        val daily = dates.map { d ->
            val orders = store.readDay(d)?.orders ?: mutableListOf()
            DailySummary(d.toString(), orders.size, orders.count { it.spicy }, orders.count { !it.spicy })
        }
        // D-005：perUser 含全部用户（0 单用户也在内），days 降序、loginName 升序
        val perUser = store.readUsersSafe().users
            .map { u ->
                var days = 0
                var spicyDays = 0
                var nonSpicyDays = 0
                dates.forEach { d ->
                    val o = store.readDay(d)?.orders?.find { it.loginName == u.loginName }
                    if (o != null) {
                        days++
                        if (o.spicy) spicyDays++ else nonSpicyDays++
                    }
                }
                UserSummary(u.loginName, u.displayName, days, spicyDays, nonSpicyDays)
            }
            .sortedWith(compareByDescending<UserSummary> { it.days }.thenBy { it.loginName })
        return RangeSummary(
            from = from.toString(),
            to = to.toString(),
            totalOrders = daily.sumOf { it.count },
            totalSpicy = daily.sumOf { it.spicy },
            totalNonSpicy = daily.sumOf { it.nonSpicy },
            daily = daily,
            perUser = perUser,
        )
    }

    /** 导出 .xlsx：Sheet1 点餐明细，Sheet2 人员汇总（契约 api.yaml） */
    fun exportXlsx(from: LocalDate, to: LocalDate): ByteArray {
        val summary = rangeSummary(from, to)
        val rangeDays = ChronoUnit.DAYS.between(from, to) + 1
        XSSFWorkbook().use { wb ->
            val detail = wb.createSheet("点餐明细")
            detail.createRow(0).let { r ->
                listOf("日期", "姓名", "登录名", "要辣", "登记时间").forEachIndexed { c, title ->
                    r.createCell(c).setCellValue(title)
                }
            }
            var row = 1
            datesBetween(from, to).forEach { d ->
                store.readDay(d)?.orders?.forEach { o ->
                    detail.createRow(row).let { r ->
                        r.createCell(0).setCellValue(d.toString())
                        r.createCell(1).setCellValue(o.displayName)
                        r.createCell(2).setCellValue(o.loginName)
                        r.createCell(3).setCellValue(if (o.spicy) "是" else "否")
                        r.createCell(4).setCellValue(o.orderedAt)
                    }
                    row++
                }
            }

            val person = wb.createSheet("人员汇总")
            person.createRow(0).let { r ->
                listOf("登录名", "姓名", "点餐天数", "要辣天数", "不要辣天数", "参与率(%)").forEachIndexed { c, title ->
                    r.createCell(c).setCellValue(title)
                }
            }
            summary.perUser.forEachIndexed { i, u ->
                person.createRow(i + 1).let { r ->
                    r.createCell(0).setCellValue(u.loginName)
                    r.createCell(1).setCellValue(u.displayName)
                    r.createCell(2).setCellValue(u.days.toDouble())
                    r.createCell(3).setCellValue(u.spicyDays.toDouble())
                    r.createCell(4).setCellValue(u.nonSpicyDays.toDouble())
                    val rate = if (rangeDays > 0) Math.round(u.days * 1000.0 / rangeDays) / 10.0 else 0.0
                    r.createCell(5).setCellValue(rate)
                }
            }

            val out = ByteArrayOutputStream()
            wb.write(out)
            return out.toByteArray()
        }
    }

    private fun requireRange(from: LocalDate, to: LocalDate) {
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) + 1 > 366) {
            throw BusinessException(1004, "日期范围非法（需 from<=to 且跨度不超过 366 天）")
        }
    }

    private fun datesBetween(from: LocalDate, to: LocalDate): List<LocalDate> =
        generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()

    @Suppress("unused")
    private fun emptyDay(date: String): DayOrders = DayOrders(date)
}
