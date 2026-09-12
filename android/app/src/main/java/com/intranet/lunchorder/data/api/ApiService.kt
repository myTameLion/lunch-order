package com.intranet.lunchorder.data.api

import com.intranet.lunchorder.data.model.ChatMessage
import com.intranet.lunchorder.data.model.ChatPostRequest
import com.intranet.lunchorder.data.model.ChatResponse
import com.intranet.lunchorder.data.model.LoginRequest
import com.intranet.lunchorder.data.model.LoginResponse
import com.intranet.lunchorder.data.model.MyOrdersResponse
import com.intranet.lunchorder.data.model.NotifySettings
import com.intranet.lunchorder.data.model.OrderRequest
import com.intranet.lunchorder.data.model.OrderView
import com.intranet.lunchorder.data.model.PasswordChangeRequest
import com.intranet.lunchorder.data.model.PasswordChangeResponse
import com.intranet.lunchorder.data.model.TodayAllResponse
import com.intranet.lunchorder.data.model.TodayStatus
import com.intranet.lunchorder.data.model.UpdateProfileRequest
import com.intranet.lunchorder.data.model.UserView
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * REST 契约（shared/contracts/api.yaml v1.1.0），仅本客户端用到的接口。
 */
interface ApiService {

    /** 登录（公开）：不自动附加 Authorization */
    @Headers("X-No-Auth: true")
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @GET("api/me")
    suspend fun me(): Response<UserView>

    @PUT("api/me/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): Response<UserView>

    @PUT("api/me/password")
    suspend fun changePassword(@Body body: PasswordChangeRequest): Response<PasswordChangeResponse>

    @GET("api/orders/today")
    suspend fun getToday(): Response<TodayStatus>

    /** 当天全员点餐情况（D-008：所有登录用户可查） */
    @GET("api/orders/today/all")
    suspend fun getTodayAll(): Response<TodayAllResponse>

    /** 我的历史点餐（D-011，缺省最近 30 天由服务端处理） */
    @GET("api/me/orders")
    suspend fun getMyOrders(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): Response<MyOrdersResponse>

    /** 公共聊天频道：当天消息（D-013，客户端仅当天） */
    @GET("api/orders/today/chat")
    suspend fun getTodayChat(): Response<ChatResponse>

    @PUT("api/orders/today/chat")
    suspend fun sendChat(@Body body: ChatPostRequest): Response<ChatMessage>

    /** 定时通知设置同步（D-014） */
    @GET("api/settings/notify")
    suspend fun getNotifySettings(): Response<NotifySettings>

    @PUT("api/orders/today")
    suspend fun putOrder(@Body body: OrderRequest): Response<OrderView>

    @DELETE("api/orders/today")
    suspend fun deleteOrder(): Response<Void>
}
