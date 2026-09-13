package com.lunchorder.web

import com.lunchorder.service.NoticeService
import com.lunchorder.web.dto.NoticesResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 重要通知（D-015）：所有登录用户可查看，顶部突出展示（折叠显示最新一条） */
@RestController
@RequestMapping("/api/notices")
class NoticeController(private val noticeService: NoticeService) {

    @GetMapping
    fun notices(): NoticesResponse = NoticesResponse(noticeService.list())
}
