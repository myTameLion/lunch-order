package com.intranet.lunchorder.data.repo

import com.intranet.lunchorder.data.api.ApiException
import com.intranet.lunchorder.data.api.ApiFactory
import com.intranet.lunchorder.data.api.ApiService
import com.intranet.lunchorder.data.api.AuthEvents
import com.intranet.lunchorder.data.api.ErrorParser
import com.intranet.lunchorder.data.model.ChatMessage
import com.intranet.lunchorder.data.model.ChatPostRequest
import com.intranet.lunchorder.data.model.ChatResponse
import com.intranet.lunchorder.data.model.LoginRequest
import com.intranet.lunchorder.data.model.LoginResponse
import com.intranet.lunchorder.data.model.MyOrdersResponse
import com.intranet.lunchorder.data.model.NoticesResponse
import com.intranet.lunchorder.data.model.NotifySettings
import com.intranet.lunchorder.data.model.OrderRequest
import com.intranet.lunchorder.data.model.OrderView
import com.intranet.lunchorder.data.model.PasswordChangeRequest
import com.intranet.lunchorder.data.model.TodayAllResponse
import com.intranet.lunchorder.data.model.TodayStatus
import com.intranet.lunchorder.data.model.UpdateProfileRequest
import com.intranet.lunchorder.data.model.UserView
import com.intranet.lunchorder.data.prefs.PrefsStore
import retrofit2.Response

/**
 * 业务仓库：统一处理错误解析、401 全局登出、token 更新。
 */
class LunchRepository(
    private val api: ApiService,
    private val prefs: PrefsStore,
) {

    /** 登录：401(1001) 由调用方提示“登录名或密码错误”，不触发全局登出 */
    suspend fun login(loginName: String, password: String): LoginResponse {
        val resp = safe { api.login(LoginRequest(loginName, password)) }
        if (resp.code() == 401) throw ErrorParser.parse(401, resp.errorBody()?.string())
        return bodyOf(resp)
    }

    suspend fun getToday(): TodayStatus = bodyOf(call { api.getToday() })

    /** 当天全员点餐情况（D-008：所有登录用户可查） */
    suspend fun getTodayAll(): TodayAllResponse = bodyOf(call { api.getTodayAll() })

    /** 我的历史点餐（D-011） */
    suspend fun getMyOrders(from: String?, to: String?): MyOrdersResponse =
        bodyOf(call { api.getMyOrders(from, to) })

    /** 公共聊天频道：当天消息（D-013，客户端仅当天） */
    suspend fun getTodayChat(): ChatResponse = bodyOf(call { api.getTodayChat() })

    suspend fun sendChat(content: String): ChatMessage = bodyOf(call { api.sendChat(ChatPostRequest(content)) })

    /** 定时通知设置同步（D-014） */
    suspend fun getNotifySettings(): NotifySettings = bodyOf(call { api.getNotifySettings() })

    /** 重要通知（D-015，最新在前） */
    suspend fun getNotices(): NoticesResponse = bodyOf(call { api.getNotices() })

    suspend fun putOrder(spicy: Boolean): OrderView = bodyOf(call { api.putOrder(OrderRequest(spicy)) })

    /** 取消点餐：成功即 204 */
    suspend fun cancelOrder() {
        call { api.deleteOrder() }
    }

    suspend fun updateProfile(displayName: String): UserView =
        bodyOf(call { api.updateProfile(UpdateProfileRequest(displayName)) })

    /** 修改密码：成功后旧 token 失效，返回并保存新 token（决策 D-004） */
    suspend fun changePassword(oldPassword: String, newPassword: String): String {
        val resp = bodyOf(call { api.changePassword(PasswordChangeRequest(oldPassword, newPassword)) })
        prefs.token = resp.token
        return resp.token
    }

    /**
     * 带鉴权的请求：HTTP 401 → 清本地 token 并发出全局登出事件，再抛出解析后的业务错误。
     */
    private suspend fun <T> call(block: suspend () -> Response<T>): Response<T> {
        val resp = safe(block)
        if (resp.code() == 401) {
            prefs.clearSession()
            AuthEvents.emitLogout()
            throw ErrorParser.parse(401, resp.errorBody()?.string())
        }
        if (!resp.isSuccessful) throw ErrorParser.parse(resp.code(), resp.errorBody()?.string())
        return resp
    }

    private fun <T> bodyOf(resp: Response<T>): T =
        resp.body() ?: throw ApiException(ApiException.CODE_UNKNOWN, "响应内容为空")

    private suspend fun <T> safe(block: suspend () -> Response<T>): Response<T> =
        try {
            block()
        } catch (t: Throwable) {
            throw ErrorParser.fromThrowable(t)
        }

    companion object {
        fun create(prefs: PrefsStore): LunchRepository = LunchRepository(ApiFactory.api(prefs), prefs)
    }
}
