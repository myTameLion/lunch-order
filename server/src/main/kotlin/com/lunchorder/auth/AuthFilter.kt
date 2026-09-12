package com.lunchorder.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.lunchorder.store.DataStore
import com.lunchorder.web.dto.ErrorResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * API 鉴权过滤器：除登录外全部校验 Bearer token；
 * token 失效/用户不存在/pwdVer 不匹配 → 401/1002；/api/admin 下还要求 ADMIN 角色 → 403/1003。
 * 通过校验后把 User 放入 request attribute "auth.user"。
 */
@Component
@Order(1)
class AuthFilter(
    private val jwtService: JwtService,
    private val dataStore: DataStore,
    private val mapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val uri = request.requestURI
        if (!uri.startsWith("/api") || uri == "/api/auth/login") {
            filterChain.doFilter(request, response)
            return
        }

        val token = request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.trim()
        val info = token?.let { jwtService.parse(it) }
        if (info == null) {
            writeError(response, 401, 1002, "登录状态无效，请重新登录")
            return
        }

        val user = dataStore.readUsersSafe().users.find { it.loginName == info.loginName }
        if (user == null || user.pwdVer != info.pwdVer) {
            writeError(response, 401, 1002, "登录状态已失效，请重新登录")
            return
        }

        if (uri.startsWith("/api/admin") && user.role != "ADMIN") {
            writeError(response, 403, 1003, "需要管理员权限")
            return
        }

        request.setAttribute(ATTR_USER, user)
        filterChain.doFilter(request, response)
    }

    private fun writeError(response: HttpServletResponse, status: Int, code: Int, message: String) {
        response.status = status
        response.characterEncoding = "UTF-8"
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write(mapper.writeValueAsString(ErrorResponse(code, message)))
    }

    companion object {
        const val ATTR_USER = "auth.user"
    }
}
