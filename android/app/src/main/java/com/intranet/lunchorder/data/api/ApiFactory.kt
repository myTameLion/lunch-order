package com.intranet.lunchorder.data.api

import com.intranet.lunchorder.data.prefs.PrefsStore
import com.intranet.lunchorder.logic.ServerUrl
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit/OkHttp 单例工厂。baseUrl 从设置读取，地址变更后自动重建。
 */
object ApiFactory {

    private const val NO_AUTH_HEADER = "X-No-Auth"

    @Volatile
    private var cached: Pair<String, ApiService>? = null

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
    }

    fun api(prefs: PrefsStore): ApiService {
        val base = ServerUrl.normalize(prefs.serverUrl)
        cached?.let { (url, service) -> if (url == base) return service }

        val okHttp = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(prefs, NO_AUTH_HEADER))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(base)
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
            .build()

        val service = retrofit.create(ApiService::class.java)
        cached = base to service
        return service
    }

    /** 规范化服务器地址（委托纯 Kotlin 实现，便于单测） */
    fun normalizeBaseUrl(url: String): String = ServerUrl.normalize(url)
}

/**
 * 自动附加 Authorization: Bearer <token>；标注 X-No-Auth 的请求（登录）跳过。
 */
class AuthInterceptor(
    private val prefs: PrefsStore,
    private val noAuthHeader: String = "X-No-Auth",
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = if (request.header(noAuthHeader) != null) {
            request.newBuilder().removeHeader(noAuthHeader)
        } else {
            val token = prefs.token
            if (token.isNotEmpty()) {
                request.newBuilder().header("Authorization", "Bearer $token")
            } else {
                request.newBuilder()
            }
        }
        return chain.proceed(builder.build())
    }
}
