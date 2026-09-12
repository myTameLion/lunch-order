package com.lunchorder.service

import com.lunchorder.domain.ChatMessage
import com.lunchorder.domain.DayMessages
import com.lunchorder.domain.User
import com.lunchorder.exception.BusinessException
import com.lunchorder.store.DataStore
import com.lunchorder.util.TimeUtil
import com.lunchorder.web.dto.ChatResponse
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * 公共聊天频道（D-013，替代 v1.2.0 的评价功能）：
 * - 所有登录用户可查看**当天**消息并发送（内容 1~200 字，追加式存储）；
 * - 历史日期仅管理员可查（/api/admin/chat）；
 * - 消息按天存 messages/YYYY-MM-DD.json，追加式，不做删除/修改。
 */
@Service
class ChatService(
    private val store: DataStore,
    private val clock: Clock,
) {

    fun todayMessages(now: ZonedDateTime): ChatResponse {
        val date = now.toLocalDate()
        val messages = store.readMessages(date)?.messages ?: mutableListOf()
        return ChatResponse(date.toString(), messages.size, messages.toList())
    }

    fun send(user: User, content: String, now: ZonedDateTime): ChatMessage {
        val text = content.trim()
        if (text.isEmpty() || text.length > 200) throw BusinessException(1004, "消息内容需为 1~200 字")
        val date = now.toLocalDate()
        return store.withLock {
            val day = store.readMessages(date) ?: DayMessages(date.toString())
            val message = ChatMessage(
                loginName = user.loginName,
                displayName = user.displayName,
                content = text,
                sentAt = TimeUtil.format(now),
            )
            day.messages.add(message)
            store.writeMessages(day)
            message
        }
    }

    /** 管理员按天查看（可查任意历史日期），按发送时间升序 */
    fun adminList(date: LocalDate): ChatResponse {
        val messages = store.readMessages(date)?.messages ?: mutableListOf()
        return ChatResponse(date.toString(), messages.size, messages.toList())
    }
}
