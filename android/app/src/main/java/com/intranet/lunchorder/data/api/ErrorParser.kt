package com.intranet.lunchorder.data.api

import com.intranet.lunchorder.data.model.ErrorResponse
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * 统一错误解析：错误体 {code, message} → ApiException。
 */
object ErrorParser {

    private val json = Json { ignoreUnknownKeys = true }

    /** 解析 HTTP 错误响应体；解析失败时退化为 HTTP 状态码错误 */
    fun parse(httpCode: Int, body: String?): ApiException {
        if (!body.isNullOrBlank()) {
            try {
                val err = json.decodeFromString(ErrorResponse.serializer(), body)
                return ApiException(err.code, err.message)
            } catch (_: Exception) {
                // 非 JSON 错误体，走兜底
            }
        }
        return ApiException(httpCode, "请求失败（HTTP $httpCode）")
    }

    /** 网络/其他异常归一化 */
    fun fromThrowable(t: Throwable): ApiException = when (t) {
        is ApiException -> t
        is IOException -> ApiException(ApiException.CODE_NETWORK, t.message ?: "无法连接服务器")
        else -> ApiException(ApiException.CODE_UNKNOWN, t.message ?: t.javaClass.simpleName)
    }
}
