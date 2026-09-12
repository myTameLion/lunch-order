package com.lunchorder.web

import com.lunchorder.auth.AuthFilter
import com.lunchorder.domain.User
import com.lunchorder.service.EvaluationService
import com.lunchorder.service.OrderService
import com.lunchorder.service.SummaryService
import com.lunchorder.web.dto.EvaluationUpsertRequest
import com.lunchorder.web.dto.EvaluationView
import com.lunchorder.web.dto.MyEvaluationResponse
import com.lunchorder.web.dto.OrderUpsertRequest
import com.lunchorder.web.dto.OrderView
import com.lunchorder.web.dto.TodayAllResponse
import com.lunchorder.web.dto.TodayStatusResponse
import com.lunchorder.web.dto.toView
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.ZonedDateTime

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService,
    private val summaryService: SummaryService,
    private val evaluationService: EvaluationService,
    private val clock: Clock,
) {

    @GetMapping("/today")
    fun today(request: HttpServletRequest): TodayStatusResponse =
        orderService.todayStatus(request.user().loginName, now())

    @PutMapping("/today")
    fun upsert(request: HttpServletRequest, @RequestBody req: OrderUpsertRequest): OrderView =
        orderService.upsert(request.user(), req.spicy, now())

    @DeleteMapping("/today")
    fun cancel(request: HttpServletRequest): ResponseEntity<Void> {
        val u = request.user()
        orderService.cancel(u.loginName, now(), u.role == "ADMIN")
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/today/all")
    fun todayAll(request: HttpServletRequest): TodayAllResponse =
        // D-008：所有登录用户都可实时查看当天点餐情况（不再限制 ADMIN）
        summaryService.todayAll(now())

    @GetMapping("/today/evaluation")
    fun myEvaluation(request: HttpServletRequest): MyEvaluationResponse =
        MyEvaluationResponse(evaluationService.myEvaluation(request.user().loginName, now())?.toView())

    @PutMapping("/today/evaluation")
    fun upsertEvaluation(request: HttpServletRequest, @RequestBody req: EvaluationUpsertRequest): EvaluationView =
        evaluationService.upsert(request.user(), req.rating, req.comment, now()).toView()

    private fun HttpServletRequest.user(): User =
        this.getAttribute(AuthFilter.ATTR_USER) as? User
            ?: throw IllegalStateException("鉴权过滤器未注入用户")

    private fun now(): ZonedDateTime = ZonedDateTime.now(clock)
}
