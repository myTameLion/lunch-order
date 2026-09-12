package com.intranet.lunchorder

import com.intranet.lunchorder.data.prefs.PrefsStore

/** 内存版 PrefsStore（纯 JVM 单测用） */
class FakePrefsStore(
    initialToken: String = "",
    initialServerUrl: String = PrefsStore.DEFAULT_SERVER_URL,
) : PrefsStore {

    var clearedSession: Boolean = false
        private set

    override var token: String = initialToken
    override var loginName: String = ""
    override var displayName: String = ""
    override var role: String = ""
    override var serverUrl: String = initialServerUrl
    override var notifyEnabled: Boolean = false
    override var notifyTime: String = PrefsStore.DEFAULT_NOTIFY_TIME
    override var notifyTitle: String = PrefsStore.DEFAULT_NOTIFY_TITLE
    override var notifyContent: String = PrefsStore.DEFAULT_NOTIFY_CONTENT

    override fun clearSession() {
        clearedSession = true
        token = ""
        loginName = ""
        displayName = ""
        role = ""
    }
}
