package com.lunchorder.exception

import org.springframework.http.HttpStatus

/** 业务异常 → 统一错误体 {code, message}（契约 error-codes.md） */
class BusinessException(
    val code: Int,
    message: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST,
) : RuntimeException(message)
