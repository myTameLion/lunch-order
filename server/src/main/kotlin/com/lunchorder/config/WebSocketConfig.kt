package com.lunchorder.config

import com.lunchorder.ws.ChatHandshakeInterceptor
import com.lunchorder.ws.ChatWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

/** WebSocket 配置：公共聊天频道 /ws/chat（D-018） */
@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val chatHandler: ChatWebSocketHandler,
    private val handshakeInterceptor: ChatHandshakeInterceptor,
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(chatHandler, "/ws/chat")
            .addInterceptors(handshakeInterceptor)
            .setAllowedOriginPatterns("*")
    }
}
