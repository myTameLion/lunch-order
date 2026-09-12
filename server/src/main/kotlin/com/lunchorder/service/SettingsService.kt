package com.lunchorder.service

import com.lunchorder.exception.BusinessException
import com.lunchorder.store.DataStore
import com.lunchorder.web.dto.NotifySettings
import org.springframework.stereotype.Service
import java.time.LocalTime

/**
 * 运行设置：定时通知（D-014）。读写 config.json；
 * Android 客户端通过 GET /api/settings/notify 同步后本地调度闹钟。
 */
@Service
class SettingsService(private val store: DataStore) {

    fun notifySettings(): NotifySettings = store.readConfig().let {
        NotifySettings(it.notifyTime, it.notifyTitle, it.notifyContent)
    }

    fun updateNotify(notifyTime: String, notifyTitle: String, notifyContent: String): NotifySettings {
        val time = runCatching { LocalTime.parse(notifyTime.trim()) }
            .getOrElse { throw BusinessException(1004, "提醒时间格式应为 HH:mm") }
        val title = notifyTitle.trim()
        if (title.isEmpty() || title.length > 30) throw BusinessException(1004, "通知标题需为 1~30 字")
        val content = notifyContent.trim()
        if (content.isEmpty() || content.length > 60) throw BusinessException(1004, "通知内容需为 1~60 字")

        store.writeConfig(store.readConfig().copy(notifyTime = time.toString(), notifyTitle = title, notifyContent = content))
        return NotifySettings(time.toString(), title, content)
    }
}
