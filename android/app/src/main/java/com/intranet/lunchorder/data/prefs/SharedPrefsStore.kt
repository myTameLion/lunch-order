package com.intranet.lunchorder.data.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences 实现（决策 D-002：Android 侧 token 存 SharedPreferences）。
 */
class SharedPrefsStore(context: Context) : PrefsStore {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var token: String
        get() = sp.getString(KEY_TOKEN, "").orEmpty()
        set(value) = sp.edit().putString(KEY_TOKEN, value).apply()

    override var loginName: String
        get() = sp.getString(KEY_LOGIN_NAME, "").orEmpty()
        set(value) = sp.edit().putString(KEY_LOGIN_NAME, value).apply()

    override var displayName: String
        get() = sp.getString(KEY_DISPLAY_NAME, "").orEmpty()
        set(value) = sp.edit().putString(KEY_DISPLAY_NAME, value).apply()

    override var role: String
        get() = sp.getString(KEY_ROLE, "").orEmpty()
        set(value) = sp.edit().putString(KEY_ROLE, value).apply()

    override var serverUrl: String
        get() = sp.getString(KEY_SERVER_URL, null) ?: PrefsStore.DEFAULT_SERVER_URL
        set(value) = sp.edit().putString(KEY_SERVER_URL, value).apply()

    override var notifyEnabled: Boolean
        get() = sp.getBoolean(KEY_NOTIFY_ENABLED, false)
        set(value) = sp.edit().putBoolean(KEY_NOTIFY_ENABLED, value).apply()

    override var notifyTime: String
        get() = sp.getString(KEY_NOTIFY_TIME, null) ?: PrefsStore.DEFAULT_NOTIFY_TIME
        set(value) = sp.edit().putString(KEY_NOTIFY_TIME, value).apply()

    override var notifyTitle: String
        get() = sp.getString(KEY_NOTIFY_TITLE, null) ?: PrefsStore.DEFAULT_NOTIFY_TITLE
        set(value) = sp.edit().putString(KEY_NOTIFY_TITLE, value).apply()

    override var notifyContent: String
        get() = sp.getString(KEY_NOTIFY_CONTENT, null) ?: PrefsStore.DEFAULT_NOTIFY_CONTENT
        set(value) = sp.edit().putString(KEY_NOTIFY_CONTENT, value).apply()

    override fun clearSession() {
        sp.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_LOGIN_NAME)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_ROLE)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "lunch_order_prefs"
        const val KEY_TOKEN = "token"
        const val KEY_LOGIN_NAME = "login_name"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_ROLE = "role"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_NOTIFY_ENABLED = "notify_enabled"
        const val KEY_NOTIFY_TIME = "notify_time"
        const val KEY_NOTIFY_TITLE = "notify_title"
        const val KEY_NOTIFY_CONTENT = "notify_content"
    }
}

/**
 * 进程级单例持有者（界面/Receiver 共用同一份配置）。
 */
object PrefsStoreProvider {
    @Volatile
    private var instance: PrefsStore? = null

    fun get(context: Context): PrefsStore =
        instance ?: synchronized(this) {
            instance ?: SharedPrefsStore(context.applicationContext).also { instance = it }
        }

    /** 仅供测试注入 */
    fun setForTest(store: PrefsStore?) {
        synchronized(this) { instance = store }
    }
}
