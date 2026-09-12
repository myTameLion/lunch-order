package com.intranet.lunchorder.logic

/**
 * 服务器地址规范化（纯 Kotlin，可单测）。
 */
object ServerUrl {

    /** 规范化：补 http:// 前缀、去首尾空白与多余斜杠；返回以 / 结尾的 baseUrl */
    fun normalize(url: String): String {
        var u = url.trim()
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
        while (u.endsWith("/")) u = u.dropLast(1)
        return "$u/"
    }

    /** 今日点餐接口完整 URL（连接测试用） */
    fun todayEndpoint(baseUrl: String): String = normalize(baseUrl) + "api/orders/today"

    /** 是否为可用的 baseUrl：能解析出 http(s) 主机即有效 */
    fun isValidBase(url: String): Boolean = try {
        val uri = java.net.URI(normalize(url))
        (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrEmpty()
    } catch (_: Exception) {
        false
    }
}
