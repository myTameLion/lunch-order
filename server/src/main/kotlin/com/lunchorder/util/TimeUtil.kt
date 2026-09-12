package com.lunchorder.util

import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TimeUtil {
    /** 全部业务逻辑固定使用 Asia/Shanghai（契约 glossary.md） */
    val SHANGHAI: ZoneId = ZoneId.of("Asia/Shanghai")

    private val OFFSET_FMT: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun format(zdt: ZonedDateTime): String = zdt.format(OFFSET_FMT)

    fun nowString(clock: Clock): String = format(ZonedDateTime.now(clock))
}
