package com.lunchorder.auth

import com.lunchorder.LunchProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

data class TokenInfo(val loginName: String, val role: String, val pwdVer: Int)

/** JWT 签发与校验（HS256，7 天；payload 含 sub/role/pwdVer，契约 D-004） */
@Component
class JwtService(props: LunchProperties) {

    private val key: SecretKey

    init {
        val bytes = props.jwtSecret.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size >= 32) { "LUNCH_JWT_SECRET 至少需要 32 字节（当前 ${bytes.size}）" }
        key = Keys.hmacShaKeyFor(bytes)
    }

    fun create(loginName: String, role: String, pwdVer: Int): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(loginName)
            .claim("role", role)
            .claim("pwdVer", pwdVer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(Duration.ofDays(7))))
            .signWith(key, Jwts.SIG.HS256)
            .compact()
    }

    /** 校验失败（伪造/过期/格式错）返回 null */
    fun parse(token: String): TokenInfo? = try {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        TokenInfo(
            loginName = claims.subject,
            role = claims.get("role", String::class.java) ?: "USER",
            pwdVer = (claims["pwdVer"] as? Number)?.toInt() ?: 0,
        )
    } catch (e: Exception) {
        null
    }
}
