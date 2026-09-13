package com.lunchorder.ws

import com.lunchorder.auth.JwtService
import com.lunchorder.store.DataStore
import org.slf4j.LoggerFactory
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

/**
 * WS 握手拦截器：从 query 参数 token 校验 JWT，
 * 校验通过把 loginName 写入 session attributes（无效则拒绝握手）。
 */
@Component
class ChatHandshakeInterceptor(
    private val jwtService: JwtService,
    private val dataStore: DataStore,
) : HandshakeInterceptor {

    private val log = LoggerFactory.getLogger(ChatHandshakeInterceptor::class.java)

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        val token = (request as? ServletServerHttpRequest)?.servletRequest
            ?.getParameter("token")
            ?.takeIf { jwtService.parse(it) != null }
            ?: run {
                log.warn("聊天 WS 握手拒绝：token 无效")
                return false
            }
        val info = jwtService.parse(token)!!
        val user = dataStore.readUsersSafe().users.find { it.loginName == info.loginName }
        if (user == null || user.pwdVer != info.pwdVer) {
            log.warn("聊天 WS 握手拒绝：用户不存在或密码版本不匹配")
            return false
        }
        attributes["loginName"] = user.loginName
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) {
        // 无需处理
    }
}
