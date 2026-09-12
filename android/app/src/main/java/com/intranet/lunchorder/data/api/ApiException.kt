package com.intranet.lunchorder.data.api

/** 统一业务异常：code/message 对应 shared/contracts/error-codes.md */
class ApiException(
    val code: Int,
    override val message: String,
) : Exception(message) {

    val isNetworkError: Boolean get() = code == CODE_NETWORK

    companion object {
        /** 本地网络异常（无法连接/超时等 IOException） */
        const val CODE_NETWORK = -1
        /** 其他未知错误 */
        const val CODE_UNKNOWN = -2
    }
}
