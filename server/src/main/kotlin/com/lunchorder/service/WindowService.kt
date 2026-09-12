package com.lunchorder.service

import com.lunchorder.LunchProperties
import com.lunchorder.domain.AppConfigJson
import com.lunchorder.exception.BusinessException
import com.lunchorder.store.DataStore
import com.lunchorder.util.TimeUtil
import com.lunchorder.web.dto.WindowInfo
import org.springframework.stereotype.Service
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * 点餐窗口：默认 [14:00, 18:00] 闭区间（含两端），Asia/Shanghai。
 * 优先级：环境变量覆盖 > config.json > 内置默认。
 */
@Service
class WindowService(
    private val dataStore: DataStore,
    private val props: LunchProperties,
) {

    fun start(): LocalTime = resolve(props.windowStart, config().orderWindowStart, "14:00")

    fun end(): LocalTime = resolve(props.windowEnd, config().orderWindowEnd, "18:00")

    /** 闭区间：14:00:00 与 18:00:00 本身都允许 */
    fun isOpen(now: ZonedDateTime): Boolean {
        val t = now.toLocalTime()
        return !t.isBefore(start()) && !t.isAfter(end())
    }

    fun windowInfo(now: ZonedDateTime): WindowInfo =
        WindowInfo(start().toString(), end().toString(), isOpen(now), TimeUtil.format(now))

    fun rejectMessage(): String = "当前不在点餐时间（${start()} - ${end()}）"

    /** 管理员设置窗口（D-009）：写入 config.json；若设置了环境变量覆盖则其仍优先 */
    fun updateWindow(start: String, end: String): Pair<LocalTime, LocalTime> {
        val s = runCatching { LocalTime.parse(start.trim()) }
            .getOrElse { throw BusinessException(1004, "开始时间格式应为 HH:mm") }
        val e = runCatching { LocalTime.parse(end.trim()) }
            .getOrElse { throw BusinessException(1004, "结束时间格式应为 HH:mm") }
        if (s.isAfter(e)) throw BusinessException(1004, "开始时间不能晚于结束时间")
        dataStore.writeConfig(dataStore.readConfig().copy(orderWindowStart = s.toString(), orderWindowEnd = e.toString()))
        return s to e
    }

    private fun config(): AppConfigJson = dataStore.readConfig()

    private fun resolve(override: String, fromConfig: String, default: String): LocalTime =
        runCatching { LocalTime.parse(override.ifBlank { fromConfig.ifBlank { default } }) }
            .getOrDefault(LocalTime.parse(default))
}
