package com.intranet.lunchorder

import com.intranet.lunchorder.logic.ReminderTime
import com.intranet.lunchorder.logic.TimeMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderTimeTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    private fun millis(date: String, time: String): Long =
        LocalDateTime.parse("${date}T$time").atZone(zone).toInstant().toEpochMilli()

    private fun assertTrigger(expectedDate: String, expectedTime: String, actualMillis: Long) {
        val expected = LocalDateTime.parse("${expectedDate}T$expectedTime").atZone(zone)
        assertEquals(expected.toInstant().toEpochMilli(), actualMillis)
    }

    @Test
    fun `当前 16点 下次触发为今天 17点30分`() {
        val now = millis("2026-09-07", "16:00:00")
        assertTrigger("2026-09-07", "17:30:00", ReminderTime.nextTriggerMillis(now, "17:30", zone))
    }

    @Test
    fun `恰好等于提醒时刻 → 明天触发`() {
        val now = millis("2026-09-07", "17:30:00")
        assertTrigger("2026-09-08", "17:30:00", ReminderTime.nextTriggerMillis(now, "17:30", zone))
    }

    @Test
    fun `当前 18点 已过提醒 → 明天 17点30分`() {
        val now = millis("2026-09-07", "18:00:00")
        assertTrigger("2026-09-08", "17:30:00", ReminderTime.nextTriggerMillis(now, "17:30", zone))
    }

    @Test
    fun `自定义时间 08点 当前 07点 → 今天 08点`() {
        val now = millis("2026-09-07", "07:00:00")
        assertTrigger("2026-09-07", "08:00:00", ReminderTime.nextTriggerMillis(now, "08:00", zone))
    }

    @Test
    fun `跨年场景`() {
        val now = millis("2026-12-31", "18:00:00")
        assertTrigger("2027-01-01", "17:30:00", ReminderTime.nextTriggerMillis(now, "17:30", zone))
    }

    @Test
    fun `月末场景`() {
        val now = millis("2026-09-30", "18:00:00")
        assertTrigger("2026-10-01", "17:30:00", ReminderTime.nextTriggerMillis(now, "17:30", zone))
    }

    @Test
    fun `时间格式校验`() {
        assertTrue(ReminderTime.isValid("17:30"))
        // ISO 时刻要求两位小时，一位小时非法（TimePicker 产出恒为两位，无影响）
        assertFalse(ReminderTime.isValid("9:05"))
        assertFalse(ReminderTime.isValid("25:00"))
        assertFalse(ReminderTime.isValid("abc"))
        assertFalse(ReminderTime.isValid(""))
    }

    @Test
    fun `parseTime 支持带空白`() {
        assertEquals(LocalTime.of(17, 30), ReminderTime.parseTime(" 17:30 "))
    }
}

class TimeMathTest {

    @Test
    fun `windowEdgeMillis 以服务器时间所属日期与时区为基准`() {
        val serverTime = "2026-09-07T15:00:00+08:00"
        val expected = LocalDateTime.parse("2026-09-07T18:00:00").atZone(ZoneId.of("Asia/Shanghai"))
            .toInstant().toEpochMilli()
        assertEquals(expected, TimeMath.windowEdgeMillis(serverTime, "18:00"))
    }

    @Test
    fun `isoToMillis`() {
        val expected = LocalDateTime.parse("2026-09-07T14:23:05").atZone(ZoneId.of("Asia/Shanghai"))
            .toInstant().toEpochMilli()
        assertEquals(expected, TimeMath.isoToMillis("2026-09-07T14:23:05+08:00"))
    }

    @Test
    fun `formatRemaining 各量级`() {
        assertEquals("0秒", TimeMath.formatRemaining(0))
        assertEquals("9秒", TimeMath.formatRemaining(9_000))
        assertEquals("5分09秒", TimeMath.formatRemaining(309_000))
        assertEquals("1小时05分09秒", TimeMath.formatRemaining(3_909_000))
    }

    @Test
    fun `负数与非法输入归零`() {
        assertEquals("0秒", TimeMath.formatRemaining(-1))
    }
}
