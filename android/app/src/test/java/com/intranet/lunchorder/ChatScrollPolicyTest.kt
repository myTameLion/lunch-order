package com.intranet.lunchorder

import com.intranet.lunchorder.logic.ChatScrollPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 聊天列表底部跟随策略（新消息仅在视口停留底部时自动滚动） */
class ChatScrollPolicyTest {

    @Test
    fun `空列表跟随`() {
        assertTrue(ChatScrollPolicy.shouldFollowBottom(0, 0))
    }

    @Test
    fun `最后一条可见 - 跟随`() {
        assertTrue(ChatScrollPolicy.shouldFollowBottom(9, 10))
    }

    @Test
    fun `倒数第二条可见 - 仍跟随（灵活容忍）`() {
        assertTrue(ChatScrollPolicy.shouldFollowBottom(8, 10))
    }

    @Test
    fun `向上翻看历史 - 不跟随`() {
        assertFalse(ChatScrollPolicy.shouldFollowBottom(5, 10))
        assertFalse(ChatScrollPolicy.shouldFollowBottom(0, 10))
    }

    @Test
    fun `自定义阈值`() {
        assertTrue(ChatScrollPolicy.shouldFollowBottom(9, 10, threshold = 1))
        assertFalse(ChatScrollPolicy.shouldFollowBottom(8, 10, threshold = 1))
    }
}
