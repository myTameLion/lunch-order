package com.intranet.lunchorder.data.model

import kotlinx.serialization.Serializable

/**
 * 与 shared/contracts/api.yaml v1.0.0 一一对应的 DTO（字段名不得私改）。
 */

@Serializable
data class LoginRequest(
    val loginName: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val token: String,
    val loginName: String,
    val displayName: String,
    val role: String,
)

@Serializable
data class UserView(
    val loginName: String,
    val displayName: String,
    val role: String,
    val createdAt: String,
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String,
)

@Serializable
data class PasswordChangeRequest(
    val oldPassword: String,
    val newPassword: String,
)

@Serializable
data class PasswordChangeResponse(
    val token: String,
)

@Serializable
data class WindowInfo(
    val start: String,
    val end: String,
    val open: Boolean,
    val serverTime: String,
)

@Serializable
data class MyOrderView(
    val spicy: Boolean,
    val orderedAt: String,
)

@Serializable
data class TodayStatus(
    val date: String,
    val window: WindowInfo,
    val myOrder: MyOrderView? = null,
)

@Serializable
data class OrderRequest(
    val spicy: Boolean,
)

@Serializable
data class OrderView(
    val loginName: String,
    val displayName: String,
    val spicy: Boolean,
    val orderedAt: String,
    val updated: Boolean,
)

/** 当天全员点餐记录项（today/all 的 orders 元素，无 updated 字段） */
@Serializable
data class TodayOrderItem(
    val loginName: String,
    val displayName: String,
    val spicy: Boolean,
    val orderedAt: String,
)

/** D-008：当天全员点餐情况，所有登录用户可查 */
@Serializable
data class TodayAllResponse(
    val date: String,
    val total: Int,
    val spicy: Int,
    val nonSpicy: Int,
    val orders: List<TodayOrderItem> = emptyList(),
)

// ── v1.2.0 ──

/** D-013：公共聊天频道（客户端仅当天） */
@Serializable
data class ChatMessage(
    val loginName: String,
    val displayName: String,
    val content: String,
    val sentAt: String,
)

@Serializable
data class ChatResponse(
    val date: String,
    val count: Int,
    val messages: List<ChatMessage> = emptyList(),
)

@Serializable
data class ChatPostRequest(val content: String)

/** D-014：定时通知设置（服务端下发，客户端同步后本地调度） */
@Serializable
data class NotifySettings(
    val notifyTime: String,
    val notifyTitle: String,
    val notifyContent: String,
)

/** D-015：重要通知（折叠显示最新一条，可展开全部） */
@Serializable
data class NoticeItem(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: String,
    val createdBy: String,
)

@Serializable
data class NoticesResponse(
    val notices: List<NoticeItem> = emptyList(),
)

/** D-011：我的历史点餐 */
@Serializable
data class MyOrderHistoryItem(
    val date: String,
    val spicy: Boolean,
    val orderedAt: String,
)

@Serializable
data class MyOrdersResponse(
    val from: String,
    val to: String,
    val records: List<MyOrderHistoryItem> = emptyList(),
    val totalDays: Int,
    val spicyDays: Int,
    val nonSpicyDays: Int,
)

/** 统一错误体 {code, message}（见 shared/contracts/error-codes.md） */
@Serializable
data class ErrorResponse(
    val code: Int,
    val message: String,
)
