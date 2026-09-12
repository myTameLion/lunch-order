package com.lunchorder

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lunch")
data class LunchProperties(
    /** 数据根目录（契约 data-file.md） */
    var dataDir: String = "./data",
    /** JWT HS256 密钥，生产必须通过环境变量注入 */
    var jwtSecret: String = "",
    /** 非空时覆盖 config.json 的窗口开始（HH:mm） */
    var windowStart: String = "",
    /** 非空时覆盖 config.json 的窗口结束（HH:mm） */
    var windowEnd: String = "",
    /** 账户密码导出功能的可逆加密密钥（AES-GCM）；缺省开发密钥并告警（D-010） */
    var exportKey: String = "",
)
