package com.intranet.lunchorder.data.prefs

/**
 * 本地存储抽象（实现：SharedPreferences；测试：内存 fake）。
 */
interface PrefsStore {
    /** JWT token，空串表示未登录 */
    var token: String
    var loginName: String
    var displayName: String
    var role: String
    var serverUrl: String
    var notifyEnabled: Boolean

    /** 提醒时间，格式 HH:mm */
    var notifyTime: String

    /** 通知标题/内容（管理员可在中台配置，客户端同步后使用） */
    var notifyTitle: String
    var notifyContent: String

    /** 退出登录：清凭证，保留服务器地址与提醒设置 */
    fun clearSession()

    companion object {
        const val DEFAULT_SERVER_URL = "http://192.168.1.100:8080"
        const val DEFAULT_NOTIFY_TIME = "17:30"
        const val DEFAULT_NOTIFY_TITLE = "午餐点餐提醒"
        const val DEFAULT_NOTIFY_CONTENT = "今天需要点餐吗？请在 18:00 前登记"
    }
}
