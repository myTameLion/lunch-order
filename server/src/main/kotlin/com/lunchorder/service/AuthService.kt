package com.lunchorder.service

import com.lunchorder.auth.JwtService
import com.lunchorder.auth.PasswordVault
import com.lunchorder.domain.User
import com.lunchorder.exception.BusinessException
import com.lunchorder.store.DataStore
import com.lunchorder.util.TimeUtil
import com.lunchorder.web.dto.LoginResponse
import com.lunchorder.web.dto.PasswordChangeRequest
import com.lunchorder.web.dto.TokenResponse
import com.lunchorder.web.dto.UserView
import com.lunchorder.web.dto.toView
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class AuthService(
    private val store: DataStore,
    private val jwtService: JwtService,
    private val vault: PasswordVault,
    private val clock: Clock,
) {

    private val encoder = BCryptPasswordEncoder()

    fun login(loginName: String, password: String): LoginResponse {
        if (loginName.isBlank() || password.isEmpty()) {
            throw BusinessException(1004, "登录名和密码不能为空")
        }
        val user = store.readUsersSafe().users.find { it.loginName == loginName }
        if (user == null || !encoder.matches(password, user.passwordHash)) {
            throw BusinessException(1001, "登录名或密码错误", org.springframework.http.HttpStatus.UNAUTHORIZED)
        }
        return LoginResponse(jwtService.create(user.loginName, user.role, user.pwdVer), user.loginName, user.displayName, user.role)
    }

    fun me(loginName: String): UserView = find(loginName).toView()

    fun updateProfile(loginName: String, displayName: String): UserView {
        val name = displayName.trim()
        if (name.isEmpty() || name.length > 20) {
            throw BusinessException(1004, "姓名需为 1~20 个字符")
        }
        return store.withLock {
            val users = store.readUsers()
            val idx = users.users.indexOfFirst { it.loginName == loginName }
            if (idx < 0) throw BusinessException(1002, "用户不存在", org.springframework.http.HttpStatus.UNAUTHORIZED)
            val updated = users.users[idx].copy(displayName = name, updatedAt = TimeUtil.nowString(clock))
            users.users[idx] = updated
            store.writeUsers(users)
            updated.toView()
        }
    }

    /** 修改密码成功后 pwdVer+1，旧 token 立即失效（D-004），返回新 token */
    fun changePassword(loginName: String, req: PasswordChangeRequest): TokenResponse {
        if (req.newPassword.length < 6 || req.newPassword.length > 64) {
            throw BusinessException(1004, "新密码长度需为 6~64 位")
        }
        return store.withLock {
            val users = store.readUsers()
            val idx = users.users.indexOfFirst { it.loginName == loginName }
            if (idx < 0) throw BusinessException(1002, "用户不存在", org.springframework.http.HttpStatus.UNAUTHORIZED)
            val user = users.users[idx]
            if (!encoder.matches(req.oldPassword, user.passwordHash)) {
                throw BusinessException(1001, "旧密码错误", org.springframework.http.HttpStatus.UNAUTHORIZED)
            }
            val updated = user.copy(
                passwordHash = encoder.encode(req.newPassword),
                passwordEnc = vault.encrypt(req.newPassword),
                pwdVer = user.pwdVer + 1,
                updatedAt = TimeUtil.nowString(clock),
            )
            users.users[idx] = updated
            store.writeUsers(users)
            TokenResponse(jwtService.create(updated.loginName, updated.role, updated.pwdVer))
        }
    }

    private fun find(loginName: String): User =
        store.readUsersSafe().users.find { it.loginName == loginName }
            ?: throw BusinessException(1002, "用户不存在", org.springframework.http.HttpStatus.UNAUTHORIZED)
}
