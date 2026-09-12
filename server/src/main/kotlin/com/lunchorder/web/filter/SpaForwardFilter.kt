package com.lunchorder.web.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * SPA history 路由回退（契约 D-006）：
 * 非 /api 的 GET、末段不含点的路径（即前端路由，而非静态资源）→
 * /admin 前缀转发到 /admin/index.html，其余转发到 /index.html（200）。
 * 静态资源（index.html、assets 下的脚本、favicon.ico 等）路径都含点，天然放行。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class SpaForwardFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val uri = request.requestURI
        val isSpaRoute = request.method == "GET" &&
            !uri.startsWith("/api") &&
            !uri.startsWith("/actuator") &&
            !uri.substringAfterLast('/').contains('.')

        if (!isSpaRoute) {
            filterChain.doFilter(request, response)
            return
        }

        val target = if (uri == "/admin" || uri.startsWith("/admin/")) "/admin/index.html" else "/index.html"
        request.getRequestDispatcher(target).forward(request, response)
    }
}
