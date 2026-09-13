package com.lunchorder.web

import com.lunchorder.exception.BusinessException
import com.lunchorder.service.ChatService
import com.lunchorder.service.OrderService
import com.lunchorder.service.NoticeService
import com.lunchorder.service.SettingsService
import com.lunchorder.service.SummaryService
import com.lunchorder.service.UserService
import com.lunchorder.web.dto.BulkCreateRequest
import com.lunchorder.web.dto.BulkCreateResponse
import com.lunchorder.web.dto.ChatResponse
import com.lunchorder.web.dto.CreateUserRequest
import com.lunchorder.web.dto.NotifySettings
import com.lunchorder.web.dto.NoticeCreateRequest
import com.lunchorder.web.dto.NotifyUpdateRequest
import com.lunchorder.web.dto.RangeSummary
import com.lunchorder.web.dto.UserView
import com.lunchorder.web.dto.UsersResponse
import com.lunchorder.web.dto.WindowSettings
import com.lunchorder.web.dto.NoticesResponse
import com.lunchorder.web.dto.WindowUpdateRequest
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val summaryService: SummaryService,
    private val userService: UserService,
    private val chatService: ChatService,
    private val orderService: OrderService,
    private val noticeService: NoticeService,
    private val settingsService: SettingsService,
    private val windowService: com.lunchorder.service.WindowService,
    private val clock: java.time.Clock,
) {

    @GetMapping("/summary")
    fun summary(@RequestParam from: String, @RequestParam to: String): RangeSummary =
        summaryService.rangeSummary(parseDate(from), parseDate(to))

    @GetMapping("/export")
    fun export(@RequestParam from: String, @RequestParam to: String, response: HttpServletResponse) {
        val bytes = summaryService.exportXlsx(parseDate(from), parseDate(to))
        val asciiName = "lunch_${from}_${to}.xlsx"
        val utf8Name = URLEncoder.encode("点餐统计_${from}_${to}.xlsx", StandardCharsets.UTF_8).replace("+", "%20")
        writeXlsx(response, bytes, asciiName, utf8Name)
    }

    @GetMapping("/users")
    fun users(): UsersResponse = UsersResponse(userService.list())

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@RequestBody req: CreateUserRequest): UserView = userService.create(req)

    /** 批量创建用户（D-010） */
    @PostMapping("/users/bulk")
    fun bulkCreateUsers(@RequestBody req: BulkCreateRequest): BulkCreateResponse = userService.bulkCreate(req)

    /** 导出全部账户与密码（D-010） */
    @GetMapping("/users/export")
    fun exportUsers(response: HttpServletResponse) {
        val today = LocalDate.now()
        val bytes = userService.exportXlsx()
        val asciiName = "accounts_$today.xlsx"
        val utf8Name = URLEncoder.encode(UserService.exportFileName(today), StandardCharsets.UTF_8).replace("+", "%20")
        writeXlsx(response, bytes, asciiName, utf8Name)
    }

    /** 点餐窗口设置（D-009） */
    @GetMapping("/settings/window")
    fun getWindow(): WindowSettings =
        WindowSettings(windowService.start().toString(), windowService.end().toString())

    @PutMapping("/settings/window")
    fun updateWindow(@RequestBody req: WindowUpdateRequest): WindowSettings {
        val (s, e) = windowService.updateWindow(req.start, req.end)
        return WindowSettings(s.toString(), e.toString())
    }

    /** 按天查看公共聊天频道历史记录（D-013，可查任意日期） */
    @GetMapping("/chat")
    fun chat(@RequestParam date: String): ChatResponse = chatService.adminList(parseDate(date))

    /** 管理员代取消指定用户当天订餐（D-017，不受窗口限制） */
    @DeleteMapping("/orders/today", params = ["loginName"])
    fun cancelForUser(@RequestParam loginName: String): ResponseEntity<Void> {
        orderService.cancel(loginName.trim(), java.time.ZonedDateTime.now(clock), isAdmin = true)
        return ResponseEntity.noContent().build()
    }

    /** 重要通知：列表 / 发布 / 删除（D-015） */
    @GetMapping("/notices")
    fun notices(): NoticesResponse = NoticesResponse(noticeService.list())

    @PostMapping("/notices")
    @ResponseStatus(HttpStatus.CREATED)
    fun createNotice(request: HttpServletRequest, @RequestBody req: NoticeCreateRequest): com.lunchorder.domain.NoticeItem =
        noticeService.create(req.title, req.content, request.admin().loginName)

    @DeleteMapping("/notices/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteNotice(@PathVariable id: Long) = noticeService.delete(id)

    /** 定时通知设置（D-014） */
    @GetMapping("/settings/notify")
    fun getNotify(): NotifySettings = settingsService.notifySettings()

    @PutMapping("/settings/notify")
    fun updateNotify(@RequestBody req: NotifyUpdateRequest): NotifySettings =
        settingsService.updateNotify(req.notifyTime, req.notifyTitle, req.notifyContent)

    private fun HttpServletRequest.admin(): com.lunchorder.domain.User =
        this.getAttribute(com.lunchorder.auth.AuthFilter.ATTR_USER) as? com.lunchorder.domain.User
            ?: throw IllegalStateException("鉴权过滤器未注入用户")

    private fun parseDate(s: String): LocalDate =
        runCatching { LocalDate.parse(s) }
            .getOrElse { throw BusinessException(1004, "日期格式应为 yyyy-MM-dd") }

    private fun writeXlsx(response: HttpServletResponse, bytes: ByteArray, asciiName: String, utf8Name: String) {
        response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        response.setHeader("Content-Disposition", "attachment; filename=\"$asciiName\"; filename*=UTF-8''$utf8Name")
        response.setContentLength(bytes.size)
        response.outputStream.write(bytes)
    }
}
