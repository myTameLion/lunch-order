package com.intranet.lunchorder.data.api

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * 全局鉴权事件总线：收到 HTTP 401（凭证失效）时发出，界面收集后跳转登录页。
 * 纯 Kotlin，可在 JVM 单测中验证。
 */
object AuthEvents {

    private val _logout = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val logout: SharedFlow<Unit> = _logout

    fun emitLogout() {
        _logout.tryEmit(Unit)
    }
}
