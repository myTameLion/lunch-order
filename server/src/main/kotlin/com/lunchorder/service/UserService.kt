package com.lunchorder.service

import com.lunchorder.auth.PasswordVault
import com.lunchorder.domain.User
import com.lunchorder.exception.BusinessException
import com.lunchorder.store.DataStore
import com.lunchorder.util.TimeUtil
import com.lunchorder.web.dto.BulkCreateRequest
import com.lunchorder.web.dto.BulkCreateResponse
import com.lunchorder.web.dto.BulkFailure
import com.lunchorder.web.dto.CreateUserRequest
import com.lunchorder.web.dto.UserView
import com.lunchorder.web.dto.toView
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.LocalDate

@Service
class UserService(
    private val store: DataStore,
    private val vault: PasswordVault,
    private val clock: Clock,
) {

    private val loginNameRegex = Regex("^[a-zA-Z0-9_]{3,20}$")
    private val encoder = BCryptPasswordEncoder()

    fun list(): List<UserView> = store.readUsersSafe().users.sortedBy { it.loginName }.map { it.toView() }

    fun create(req: CreateUserRequest): UserView {
        val login = req.loginName.trim()
        if (!loginNameRegex.matches(login)) throw BusinessException(1004, "登录名需为 3~20 位字母/数字/下划线")
        val name = req.displayName.trim()
        if (!isValidName(name)) throw BusinessException(1004, "姓名需为 1~20 个字符")
        if (req.password.length < 6 || req.password.length > 64) throw BusinessException(1004, "密码长度需为 6~64 位")
        val role = req.role ?: "USER"
        if (role != "USER" && role != "ADMIN") throw BusinessException(1004, "角色只能是 USER 或 ADMIN")

        return store.withLock {
            val users = store.readUsers()
            if (users.users.any { it.loginName == login }) {
                throw BusinessException(1005, "登录名已存在", HttpStatus.CONFLICT)
            }
            val user = buildUser(login, name, req.password, role)
            users.users.add(user)
            store.writeUsers(users)
            user.toView()
        }
    }

    /**
     * 批量创建（D-010）：逐条校验，失败条目不影响成功条目；
     * 批内重复/与现有重复按条目失败；密码缺省 123456。
     */
    fun bulkCreate(req: BulkCreateRequest): BulkCreateResponse {
        return store.withLock {
            val users = store.readUsers()
            val batchSeen = mutableSetOf<String>()
            val failed = mutableListOf<BulkFailure>()
            var created = 0
            val now = TimeUtil.nowString(clock)

            for (item in req.users) {
                val login = item.loginName.trim()
                val name = item.displayName.trim()
                val pwd = item.password ?: DEFAULT_BULK_PASSWORD
                val reason = when {
                    !loginNameRegex.matches(login) -> "登录名需为 3~20 位字母/数字/下划线"
                    login in batchSeen -> "批内登录名重复"
                    users.users.any { it.loginName == login } -> "登录名已存在"
                    !isValidName(name) -> "姓名需为 1~20 个字符"
                    pwd.length < 6 || pwd.length > 64 -> "密码长度需为 6~64 位"
                    else -> null
                }
                if (reason != null) {
                    failed.add(BulkFailure(login, reason))
                    continue
                }
                batchSeen.add(login)
                users.users.add(buildUser(login, name, pwd, "USER"))
                created++
            }
            if (created > 0) store.writeUsers(users)
            BulkCreateResponse(created, failed)
        }
    }

    /** 导出全部账户 xlsx（D-010）：密码为可逆解密结果；存量无 passwordEnc 的账号显示"—" */
    fun exportXlsx(): ByteArray {
        val users = store.readUsersSafe().users.sortedBy { it.loginName }
        XSSFWorkbook().use { wb ->
            val sheet = wb.createSheet("账户列表")
            sheet.createRow(0).let { r ->
                listOf("登录名", "姓名", "角色", "密码", "创建时间").forEachIndexed { c, title ->
                    r.createCell(c).setCellValue(title)
                }
            }
            users.forEachIndexed { i, u ->
                sheet.createRow(i + 1).let { r ->
                    r.createCell(0).setCellValue(u.loginName)
                    r.createCell(1).setCellValue(u.displayName)
                    r.createCell(2).setCellValue(if (u.role == "ADMIN") "管理员" else "员工")
                    r.createCell(3).setCellValue(vault.decrypt(u.passwordEnc) ?: "—")
                    r.createCell(4).setCellValue(u.createdAt)
                }
            }
            val out = ByteArrayOutputStream()
            wb.write(out)
            return out.toByteArray()
        }
    }

    private fun buildUser(login: String, name: String, pwd: String, role: String): User {
        val now = TimeUtil.nowString(clock)
        return User(
            loginName = login,
            displayName = name,
            passwordHash = encoder.encode(pwd),
            passwordEnc = vault.encrypt(pwd),
            pwdVer = 1,
            role = role,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun isValidName(s: String): Boolean {
        val t = s.trim()
        return t.isNotEmpty() && t.length <= 20
    }

    companion object {
        const val DEFAULT_BULK_PASSWORD = "123456"

        /** 供导出文件命名使用 */
        fun exportFileName(date: LocalDate): String = "账号列表_$date.xlsx"
    }
}
