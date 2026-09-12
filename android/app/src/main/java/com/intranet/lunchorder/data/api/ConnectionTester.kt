package com.intranet.lunchorder.data.api

import com.intranet.lunchorder.logic.ServerUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 服务器连通性测试：向 {base}/api/orders/today 发无 token 请求，
 * 收到任意 HTTP 响应（含 401/404/409）即判定可达；IOException 判定不可达。
 * 纯 JVM 实现，可用 MockWebServer 单测。
 */
object ConnectionTester {

    data class Result(
        val ok: Boolean,
        /** ok=true: HTTP 状态描述；ok=false: 失败原因 */
        val detail: String,
    )

    fun test(baseUrl: String, timeoutMillis: Long = 4000): Result {
        val url = try {
            ServerUrl.todayEndpoint(baseUrl)
        } catch (e: Exception) {
            return Result(ok = false, detail = e.message ?: "地址无效")
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            .build()
        return try {
            // 非法地址（如空主机/坏端口）会在构建 Request 时抛出，必须一并捕获
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { resp ->
                Result(ok = true, detail = "HTTP ${resp.code}")
            }
        } catch (e: IOException) {
            Result(ok = false, detail = e.message ?: e.javaClass.simpleName)
        } catch (e: IllegalArgumentException) {
            Result(ok = false, detail = e.message ?: "地址无效")
        }
    }
}
