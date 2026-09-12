package com.lunchorder.web

import com.lunchorder.auth.AuthFilter
import com.lunchorder.domain.User
import com.lunchorder.exception.BusinessException
import com.lunchorder.service.AuthService
import com.lunchorder.service.OrderService
import com.lunchorder.web.dto.MyOrdersResponse
import com.lunchorder.web.dto.PasswordChangeRequest
import com.lunchorder.web.dto.ProfileRequest
import com.lunchorder.web.dto.TokenResponse
import com.lunchorder.web.dto.UserView
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.LocalDate

@RestController
@RequestMapping("/api/me")
class MeController(
    private val authService: AuthService,
    private val orderService: OrderService,
    private val clock: Clock,
) {

    @GetMapping
    fun me(request: HttpServletRequest): UserView = authService.me(request.user().loginName)

    /** 我的历史点餐（D-011）：缺省最近 30 天 */
    @GetMapping("/orders")
    fun myOrders(
        request: HttpServletRequest,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
    ): MyOrdersResponse {
        val today = LocalDate.now(clock)
        val f = from?.let { parseDate(it) } ?: today.minusDays(29)
        val t = to?.let { parseDate(it) } ?: today
        return orderService.myOrders(request.user().loginName, f, t)
    }

    @PutMapping("/profile")
    fun updateProfile(request: HttpServletRequest, @RequestBody req: ProfileRequest): UserView =
        authService.updateProfile(request.user().loginName, req.displayName)

    @PutMapping("/password")
    fun changePassword(request: HttpServletRequest, @RequestBody req: PasswordChangeRequest): TokenResponse =
        authService.changePassword(request.user().loginName, req)

    private fun parseDate(s: String): LocalDate =
        runCatching { LocalDate.parse(s) }
            .getOrElse { throw BusinessException(1004, "日期格式应为 yyyy-MM-dd") }

    private fun HttpServletRequest.user(): User =
        this.getAttribute(AuthFilter.ATTR_USER) as? User
            ?: throw IllegalStateException("鉴权过滤器未注入用户")
}
