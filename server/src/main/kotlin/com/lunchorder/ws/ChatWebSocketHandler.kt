package com.lunchorder.ws

import com.fasterxml.jackson.databind.ObjectMapper
import com.lunchorder.domain.User
import com.lunchorder.service.ChatService
import com.lunchorder.store.DataStore
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * 公共聊天频道 WebSocket（D-018）：
 * - 握手时已由 HandshakeInterceptor 校验 token 并把 User 放入 session attributes；
 * - 客户端发送 {"content":"..."} → 持久化并广播 {"type":"chat","message":{...}}；
 * - 校验失败回发 {"type":"error","message":...}（不关闭连接）。
 */
@Component
class ChatWebSocketHandler(
    private val chatService: ChatService,
    private val store: DataStore,
    private val mapper: ObjectMapper,
    private val clock: java.time.Clock,
) : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(ChatWebSocketHandler::class.java)

    /** 在线会话：session → 登录名 */
    private val sessions = ConcurrentHashMap<WebSocketSession, String>()

    val onlineCount: Int get() = sessions.size

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val user = session.userOrNull() ?: run { session.close(CloseStatus.POLICY_VIOLATION); return }
        sessions[session] = user.loginName
        log.info("聊天 WS 连接建立：{}（在线 {} 人）", user.loginName, sessions.size)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val login = sessions.remove(session)
        if (login != null) log.info("聊天 WS 连接关闭：{}（在线 {} 人）", login, sessions.size)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val user = session.userOrNull() ?: return
        val content = runCatching {
            mapper.readTree(message.payload).get("content")?.asText()
        }.getOrNull()
        if (content.isNullOrBlank()) {
            sendError(session, "消息内容不能为空")
            return
        }
        try {
            val saved = chatService.send(user, content, ZonedDateTime.now(clock))
            broadcast(mapper.writeValueAsString(mapOf("type" to "chat", "message" to saved)))
        } catch (e: com.lunchorder.exception.BusinessException) {
            sendError(session, e.message ?: "发送失败")
        }
    }

    private fun broadcast(payload: String) {
        val text = TextMessage(payload)
        sessions.keys.forEach { session ->
            runCatching { session.sendMessage(text) }
                .onFailure { log.warn("WS 广播失败：{}", it.message) }
        }
    }

    private fun sendError(session: WebSocketSession, message: String) {
        runCatching {
            session.sendMessage(TextMessage(mapper.writeValueAsString(mapOf("type" to "error", "message" to message))))
        }
    }

    private fun WebSocketSession.userOrNull(): User? {
        val loginName = attributes["loginName"] as? String ?: return null
        return store.readUsersSafe().users.find { it.loginName == loginName }
    }

}
