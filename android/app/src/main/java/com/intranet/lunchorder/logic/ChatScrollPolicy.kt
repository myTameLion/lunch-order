package com.intranet.lunchorder.logic

/**
 * 聊天列表滚动策略（F-CHAT-03/04）：
 * - 最后可见条目位于列表尾部（倒数 threshold 条内，灵活容忍）→ 视为在底部，新消息自动跟随滚动；
 * - 用户向上翻看历史 → 不跟随。
 * 纯函数便于 JVM 单测。
 */
object ChatScrollPolicy {

    const val DEFAULT_THRESHOLD = 2

    fun shouldFollowBottom(lastVisiblePosition: Int, itemCount: Int, threshold: Int = DEFAULT_THRESHOLD): Boolean {
        if (itemCount <= 0) return true
        return lastVisiblePosition >= itemCount - threshold
    }
}
