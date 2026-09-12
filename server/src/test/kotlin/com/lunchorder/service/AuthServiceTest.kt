package com.lunchorder.service

import com.lunchorder.auth.JwtService
import com.lunchorder.auth.PasswordVault
import com.lunchorder.LunchProperties
import com.lunchorder.exception.BusinessException
import com.lunchorder.store.DataStore
import com.lunchorder.util.TimeUtil
import com.lunchorder.web.dto.PasswordChangeRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.util.UUID

class AuthServiceTest {

    @TempDir
    lateinit var temp: Path

    private lateinit var store: DataStore
    private lateinit var auth: AuthService
    private lateinit var jwt: JwtService
    private lateinit var vault: PasswordVault

    private val clock: Clock = Clock.fixed(Instant.parse("2026-09-05T07:00:00Z"), TimeUtil.SHANGHAI)

    @BeforeEach
    fun setup() {
        store = DataStore(LunchProperties(dataDir = temp.resolve("a-${UUID.randomUUID()}").toString()))
        jwt = JwtService(LunchProperties(jwtSecret = "0123456789abcdef0123456789abcdef"))
        vault = PasswordVault(LunchProperties())
        auth = AuthService(store, jwt, vault, clock)
    }

    @Test
    fun `登录成功返回 token 与身份`() {
        val resp = auth.login("admin", "admin123")
        assertEquals("ADMIN", resp.role)
        assertEquals("管理员", resp.displayName)
        val info = jwt.parse(resp.token)!!
        assertEquals("admin", info.loginName)
        assertEquals(1, info.pwdVer)
    }

    @Test
    fun `密码错误 - 1001`() {
        assertEquals(1001, assertThrows<BusinessException> { auth.login("admin", "bad") }.code)
    }

    @Test
    fun `登录名不存在 - 1001`() {
        assertEquals(1001, assertThrows<BusinessException> { auth.login("nobody", "123456") }.code)
    }

    @Test
    fun `空参数 - 1004`() {
        assertEquals(1004, assertThrows<BusinessException> { auth.login("", "") }.code)
    }

    @Test
    fun `修改密码全流程 - 旧密码错 1001、过短 1004、成功后 pwdVer 递增`() {
        val oldToken = auth.login("zhangsan", "123456").token

        assertEquals(1001, assertThrows<BusinessException> {
            auth.changePassword("zhangsan", PasswordChangeRequest("wrong", "abcdef88"))
        }.code)
        assertEquals(1004, assertThrows<BusinessException> {
            auth.changePassword("zhangsan", PasswordChangeRequest("123456", "123"))
        }.code)

        val resp = auth.changePassword("zhangsan", PasswordChangeRequest("123456", "abcdef88"))
        val user = store.readUsers().users.first { it.loginName == "zhangsan" }
        assertEquals(2, user.pwdVer)
        assertEquals(2, jwt.parse(resp.token)!!.pwdVer)
        // 可逆加密密码同步更新（D-010）
        assertEquals("abcdef88", vault.decrypt(user.passwordEnc))
        // 旧 token 签名仍合法但 pwdVer 已过期（过滤器层会判 1002）
        assertEquals(1, jwt.parse(oldToken)!!.pwdVer)
        // 新密码可登录
        assertEquals("zhangsan", auth.login("zhangsan", "abcdef88").loginName)
    }

    @Test
    fun `修改姓名 - 去空白生效且校验长度`() {
        val view = auth.updateProfile("zhangsan", "  张三丰  ")
        assertEquals("张三丰", view.displayName)
        assertEquals("张三丰", store.readUsers().users.first { it.loginName == "zhangsan" }.displayName)

        assertEquals(1004, assertThrows<BusinessException> { auth.updateProfile("zhangsan", "   ") }.code)
        val tooLong = "一二三四五六七八九十一二三四五六七八九十1"
        assertEquals(21, tooLong.length)
        assertEquals(1004, assertThrows<BusinessException> { auth.updateProfile("zhangsan", tooLong) }.code)
        assertTrue(true)
    }
}
