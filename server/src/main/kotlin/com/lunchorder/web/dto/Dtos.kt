package com.lunchorder.web.dto

import com.lunchorder.domain.OrderRecord
import com.lunchorder.domain.User

data class ErrorResponse(val code: Int, val message: String)

data class LoginRequest(val loginName: String = "", val password: String = "")
data class LoginResponse(val token: String, val loginName: String, val displayName: String, val role: String)

data class UserView(val loginName: String, val displayName: String, val role: String, val createdAt: String)
fun User.toView() = UserView(loginName, displayName, role, createdAt)

data class ProfileRequest(val displayName: String = "")
data class PasswordChangeRequest(val oldPassword: String = "", val newPassword: String = "")
data class TokenResponse(val token: String)

data class WindowInfo(val start: String, val end: String, val open: Boolean, val serverTime: String)
data class MyOrderView(val spicy: Boolean, val orderedAt: String)
data class TodayStatusResponse(val date: String, val window: WindowInfo, val myOrder: MyOrderView?)

data class OrderUpsertRequest(val spicy: Boolean = false)
data class OrderView(val loginName: String, val displayName: String, val spicy: Boolean, val orderedAt: String, val updated: Boolean)

/** orders 列表直接复用文件层的 OrderRecord，字段与契约一致 */
data class TodayAllResponse(
    val date: String,
    val window: WindowInfo,
    val total: Int,
    val spicy: Int,
    val nonSpicy: Int,
    val orders: List<OrderRecord>,
)

data class CreateUserRequest(
    val loginName: String = "",
    val displayName: String = "",
    val password: String = "",
    val role: String? = null,
)
data class UsersResponse(val users: List<UserView>)

data class DailySummary(val date: String, val count: Int, val spicy: Int, val nonSpicy: Int)
data class UserSummary(val loginName: String, val displayName: String, val days: Int, val spicyDays: Int, val nonSpicyDays: Int)
data class RangeSummary(
    val from: String,
    val to: String,
    val totalOrders: Int,
    val totalSpicy: Int,
    val totalNonSpicy: Int,
    val daily: List<DailySummary>,
    val perUser: List<UserSummary>,
)

// ── v1.2.0 ──

data class WindowSettings(val orderWindowStart: String, val orderWindowEnd: String)
data class WindowUpdateRequest(val start: String = "", val end: String = "")

data class BulkUserItem(val loginName: String = "", val displayName: String = "", val password: String? = null)
data class BulkCreateRequest(val users: List<BulkUserItem> = emptyList())
data class BulkFailure(val loginName: String, val reason: String)
data class BulkCreateResponse(val created: Int, val failed: List<BulkFailure>)

data class MyOrderHistoryItem(val date: String, val spicy: Boolean, val orderedAt: String)
data class MyOrdersResponse(
    val from: String,
    val to: String,
    val records: List<MyOrderHistoryItem>,
    val totalDays: Int,
    val spicyDays: Int,
    val nonSpicyDays: Int,
)

// ── v1.3.0 ──

data class ChatPostRequest(val content: String = "")
data class ChatResponse(val date: String, val count: Int, val messages: List<com.lunchorder.domain.ChatMessage>)
data class NotifySettings(val notifyTime: String, val notifyTitle: String, val notifyContent: String)
data class NotifyUpdateRequest(val notifyTime: String = "", val notifyTitle: String = "", val notifyContent: String = "")

// ── v1.4.0 ──

data class NoticeCreateRequest(val title: String = "", val content: String = "")
data class NoticesResponse(val notices: List<com.lunchorder.domain.NoticeItem>)
