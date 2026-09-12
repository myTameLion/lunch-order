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

/** evaluations/YYYY-MM-DD.json 中的一条评价（D-012；每用户每天仅保留最后一次） */
data class EvaluationRecord(
    val loginName: String,
    val displayName: String,
    val rating: Int,
    val comment: String = "",
    val ratedAt: String,
)

data class DayEvaluations(val date: String, val evaluations: MutableList<EvaluationRecord> = mutableListOf())

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
)
