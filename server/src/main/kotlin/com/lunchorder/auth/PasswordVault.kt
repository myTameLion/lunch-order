package com.lunchorder.auth

import com.lunchorder.LunchProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 账户密码的可逆加密存储（D-010）：
 * 仅为满足"导出全部账户与密码"需求；登录校验仍使用 BCrypt。
 * AES-256-GCM，密钥由 LUNCH_EXPORT_KEY 派生（SHA-256），缺省开发密钥并告警。
 * 安全权衡：可逆存储密码天然降低安全性，仅适合内网信任环境。
 */
@Component
class PasswordVault(props: LunchProperties) {

    private val log = LoggerFactory.getLogger(PasswordVault::class.java)
    private val key: SecretKeySpec
    private val random = SecureRandom()

    init {
        val raw = props.exportKey.ifBlank {
            log.warn("未设置 LUNCH_EXPORT_KEY，账户密码导出功能使用内置开发密钥（仅限开发/演示）")
            DEV_KEY
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        key = SecretKeySpec(digest, "AES")
    }

    fun encrypt(plain: String): String {
        val iv = ByteArray(IV_LEN).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + encrypted)
    }

    /** 解密失败（密钥不匹配/数据损坏）返回 null，导出时显示"—" */
    fun decrypt(encoded: String?): String? {
        if (encoded.isNullOrBlank()) return null
        return try {
            val data = Base64.getDecoder().decode(encoded)
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, data.copyOfRange(0, IV_LEN)))
            String(cipher.doFinal(data.copyOfRange(IV_LEN, data.size)), Charsets.UTF_8)
        } catch (e: Exception) {
            log.warn("passwordEnc 解密失败（密钥可能已更换）")
            null
        }
    }

    private companion object {
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val IV_LEN = 12
        const val DEV_KEY = "lunch-order-dev-export-key-change-me"
    }
}
