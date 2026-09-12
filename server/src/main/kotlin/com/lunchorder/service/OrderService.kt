package com.lunchorder.service

import com.lunchorder.domain.DayOrders
import com.lunchorder.domain.User
import com.lunchorder.exception.BusinessException
import com.lunchorder.store.DataStore
import com.lunchorder.store.newOrder
import com.lunchorder.util.TimeUtil
import com.lunchorder.web.dto.MyOrderHistoryItem
import com.lunchorder.web.dto.MyOrdersResponse
import com.lunchorder.web.dto.MyOrderView
import com.lunchorder.web.dto.OrderView
import com.lunchorder.web.dto.TodayStatusResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Service
class OrderService(
    private val store: DataStore,
    private val windowService: WindowService,
    private val clock: Clock,
) {

    fun todayStatus(loginName: String, now: ZonedDateTime): TodayStatusResponse {
        val date = now.toLocalDate()
        val mine = store.readDay(date)?.orders?.find { it.loginName == loginName }
        return TodayStatusResponse(
            date = date.toString(),
            window = windowService.windowInfo(now),
            myOrder = mine?.let { MyOrderView(it.spicy, it.orderedAt) },
        )
    }

    /** 登记/修改：每人每天一条；重复提交=修改；窗口外拒绝（管理员不受限，D-003） */
    fun upsert(user: User, spicy: Boolean, now: ZonedDateTime): OrderView {
        val date = now.toLocalDate()
        if (user.role != "ADMIN" && !windowService.isOpen(now)) {
            throw BusinessException(2001, windowService.rejectMessage(), HttpStatus.CONFLICT)
        }
        return store.withLock {
            val day = store.readDay(date) ?: DayOrders(date.toString())
            val at = TimeUtil.format(now)
            val existing = day.orders.find { it.loginName == user.loginName }
            var updated = false
            if (existing != null) {
                day.orders[day.orders.indexOf(existing)] =
                    existing.copy(spicy = spicy, displayName = user.displayName, orderedAt = at)
                updated = true
            } else {
                day.orders.add(newOrder(user.loginName, user.displayName, spicy, at))
            }
            store.writeDay(day)
            OrderView(user.loginName, user.displayName, spicy, at, updated)
        }
    }

    fun cancel(loginName: String, now: ZonedDateTime, isAdmin: Boolean) {
        val date = now.toLocalDate()
        if (!isAdmin && !windowService.isOpen(now)) {
            throw BusinessException(2001, windowService.rejectMessage(), HttpStatus.CONFLICT)
        }
        store.withLock {
            val day = store.readDay(date) ?: throw BusinessException(2002, "今天还没有你的点餐记录", HttpStatus.NOT_FOUND)
            val removed = day.orders.removeIf { it.loginName == loginName }
            if (!removed) throw BusinessException(2002, "今天还没有你的点餐记录", HttpStatus.NOT_FOUND)
            store.writeDay(day)
        }
    }

    /** 我的历史点餐（D-011）：逐日记录 + 汇总 */
    fun myOrders(loginName: String, from: LocalDate, to: LocalDate): MyOrdersResponse {
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) + 1 > 366) {
            throw BusinessException(1004, "日期范围非法（需 from<=to 且跨度不超过 366 天）")
        }
        val records = mutableListOf<MyOrderHistoryItem>()
        var total = 0
        var spicy = 0
        var nonSpicy = 0
        var d = from
        while (!d.isAfter(to)) {
            val mine = store.readDay(d)?.orders?.find { it.loginName == loginName }
            if (mine != null) {
                records.add(MyOrderHistoryItem(d.toString(), mine.spicy, mine.orderedAt))
                total++
                if (mine.spicy) spicy++ else nonSpicy++
            }
            d = d.plusDays(1)
        }
        return MyOrdersResponse(from.toString(), to.toString(), records, total, spicy, nonSpicy)
    }
}
