package com.intranet.lunchorder.logic

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 每日提醒的下次触发时间计算（纯 Kotlin，便于 JVM 单测）。
 *
 * 规则：输入当前毫秒与 "HH:mm"，输出下次触发毫秒——
 * 今天该时刻已过（含恰好等于当前时刻）→ 明天该时刻；否则今天该时刻。
 */
object ReminderTime {

    /** 解析 "HH:mm" 非法时抛 IllegalArgumentException */
    fun parseTime(timeOfDay: String): LocalTime = LocalTime.parse(timeOfDay.trim())

    /** "HH:mm" 是否合法 */
    fun isValid(timeOfDay: String): Boolean = try {
        parseTime(timeOfDay); true
    } catch (_: Exception) {
        false
    }

    /**
     * 下次触发时间（epoch 毫秒）。
     * @param nowMillis 当前时间毫秒
     * @param timeOfDay "HH:mm"
     * @param zone 计算时区，默认系统时区
     */
    fun nextTriggerMillis(
        nowMillis: Long,
        timeOfDay: String,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val time = parseTime(timeOfDay)
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
        val todayTrigger = now.toLocalDate().atTime(time).atZone(zone)
        val next = if (todayTrigger.toInstant().toEpochMilli() <= nowMillis) {
            // 今天已过（或恰好是当前时刻）→ 明天
            now.toLocalDate().plusDays(1).atTime(time).atZone(zone)
        } else {
            todayTrigger
        }
        return next.toInstant().toEpochMilli()
    }
}

/**
 * 点餐窗口相关时间换算（纯 Kotlin）。
 */
object TimeMath {

    /**
     * 计算窗口边界（start/end，"HH:mm"）对应的服务器时刻（epoch 毫秒），
     * 以 serverTime（ISO 8601 带时区）所属日期与时区为基准。
     */
    fun windowEdgeMillis(serverTimeIso: String, hhmm: String): Long {
        val odt = java.time.OffsetDateTime.parse(serverTimeIso.trim())
        val edge = odt.toLocalDate().atTime(LocalTime.parse(hhmm.trim())).atOffset(odt.offset)
        return edge.toInstant().toEpochMilli()
    }

    /** ISO 8601 带时区时间字符串 → epoch 毫秒 */
    fun isoToMillis(iso: String): Long = java.time.OffsetDateTime.parse(iso.trim()).toInstant().toEpochMilli()

    /** 剩余毫秒 → "1小时05分09秒" / "05分09秒" / "09秒" */
    fun formatRemaining(remainingMillis: Long): String {
        if (remainingMillis <= 0) return "0秒"
        var rem = remainingMillis / 1000
        val h = Duration.ofSeconds(rem).toHours()
        rem -= h * 3600
        val m = rem / 60
        val s = rem % 60
        return when {
            h > 0 -> "%d小时%02d分%02d秒".format(h, m, s)
            m > 0 -> "%d分%02d秒".format(m, s)
            else -> "%d秒".format(s)
        }
    }
}
