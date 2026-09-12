package com.lunchorder.web

import com.lunchorder.service.SettingsService
import com.lunchorder.web.dto.NotifySettings
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 登录客户端的运行设置同步（D-014）：Android 拉取定时通知配置后本地调度 */
@RestController
@RequestMapping("/api/settings")
class SettingsController(private val settingsService: SettingsService) {

    @GetMapping("/notify")
    fun notify(): NotifySettings = settingsService.notifySettings()
}
