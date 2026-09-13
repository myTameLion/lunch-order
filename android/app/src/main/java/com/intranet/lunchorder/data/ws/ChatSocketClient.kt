package com.intranet.lunchorder.data.ws

import com.intranet.lunchorder.data.model.ChatMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * 公共聊天频道 WebSocket 客户端（D-018）：
 * - 连接 `ws(s)://<host>/ws/chat?token=<JWT>`；
 * - 发送 {"content":"..."}；接收广播 {"type":"chat","message":{...}} / {"type":"error","message":...}；
 * - 断线后 3 秒自动重连（close() 主动关闭不重连）。
 */
class ChatSocketClient(
    private val baseUrl: String,
    private val token: String,
    private val listener: Listener,
) {

    interface Listener {
        fun onChatMessage(message: ChatMessage)
        fun onError(message: String)
        fun onClosedTemporarily()
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private var ws: WebSocket? = null
    @Volatile
    private var closedByUser = false

    fun connect() {
        closedByUser = false
        val wsUrl = baseUrl
            .replaceFirst("http://", "ws://")
            .replaceFirst("https://", "wss://")
        val request = Request.Builder().url("${wsUrl}ws/chat?token=$token").build()
        ws = client.newWebSocket(request, InnerListener())
    }

    /** @return true=已通过 WS 发送 */
    fun send(content: String): Boolean {
        val socket = ws ?: return false
        val frame = json.encodeToString(ChatSendFrame.serializer(), ChatSendFrame(content))
        return socket.send(frame)
    }

    fun close() {
        closedByUser = true
        ws?.close(1000, "bye")
        ws = null
    }

    private inner class InnerListener : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val frame = json.decodeFromString(ChatFrame.serializer(), text)
                when (frame.type) {
                    "chat" -> frame.message?.let { listener.onChatMessage(json.decodeFromJsonElement(ChatMessage.serializer(), it)) }
                    "error" -> listener.onError((frame.message as? JsonPrimitive)?.content ?: "发送失败")
                }
            } catch (_: Exception) {
                /* 非 JSON 帧忽略 */
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (!closedByUser) listener.onClosedTemporarily()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (!closedByUser) listener.onClosedTemporarily()
        }
    }

    @kotlinx.serialization.Serializable
    data class ChatFrame(val type: String, val message: JsonElement? = null)

    @kotlinx.serialization.Serializable
    data class ChatSendFrame(val content: String)
}
