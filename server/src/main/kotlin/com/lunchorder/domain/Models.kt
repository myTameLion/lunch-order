package com.lunchorder.domain

/** users.json 中的用户账号（契约 data-file.md） */
data class User(
    val loginName: String,
    val displayName: String,
    val passwordHash: String,
    /** 可逆加密的密码（D-010，导出账户用）；存量账号可能为 null */
    val passwordEnc: String? = null,
    val pwdVer: Int = 1,
    val role: String = "USER",
    val createdAt: String,
    val updatedAt: String,
)

data class UsersFile(val users: MutableList<User> = mutableListOf())

/** messages/YYYY-MM-DD.json 中的一条聊天消息（D-013 公共聊天频道，追加式存储） */
data class ChatMessage(
    val loginName: String,
    val displayName: String,
    val content: String,
    val sentAt: String,
)

data class DayMessages(val date: String, val messages: MutableList<ChatMessage> = mutableListOf())

/** notices.json 中的一条重要通知（D-015；自增 id，展示按 id 倒序=最新在前） */
data class NoticeItem(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: String,
    val createdBy: String,
)

data class NoticesFile(val notices: MutableList<NoticeItem> = mutableListOf())

/** orders/YYYY-MM-DD.json 中的一条点餐记录；时间一律 ISO-8601 带 +08:00 偏移的字符串 */
data class OrderRecord(
    val loginName: String,
    val displayName: String,
    val spicy: Boolean,
    val orderedAt: String,
)

data class DayOrders(val date: String, val orders: MutableList<OrderRecord> = mutableListOf())

/** config.json；字段名与文件一致 */
data class AppConfigJson(
    val orderWindowStart: String = "14:00",
    val orderWindowEnd: String = "18:00",
    val notifyTime: String = "17:30",
    val notifyTitle: String = "午餐点餐提醒",
    val notifyContent: String = "今天需要点餐吗？请在 18:00 前登记",
)
