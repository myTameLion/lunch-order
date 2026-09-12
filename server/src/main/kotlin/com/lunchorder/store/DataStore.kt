package com.lunchorder.store

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.lunchorder.LunchProperties
import com.lunchorder.auth.PasswordVault
import com.lunchorder.domain.AppConfigJson
import com.lunchorder.domain.DayEvaluations
import com.lunchorder.domain.DayOrders
import com.lunchorder.domain.EvaluationRecord
import com.lunchorder.domain.OrderRecord
import com.lunchorder.domain.User
import com.lunchorder.domain.UsersFile
import com.lunchorder.util.TimeUtil
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

/**
 * JSON 文件存储（契约 data-file.md）：
 * - 原子写：同目录临时文件 + ATOMIC_MOVE
 * - 进程内全局写锁（synchronized 可重入，服务层读改写在 withLock 中完成）
 * - 启动校验/种子初始化；单文件损坏不崩溃（告警 + 按空处理/重新种子）
 */
@Component
class DataStore(props: LunchProperties, private val vault: PasswordVault? = null) {

    private val log = LoggerFactory.getLogger(DataStore::class.java)

    private val mapper = ObjectMapper()
        .registerKotlinModule()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    private val dataDir: Path = Path.of(props.dataDir).toAbsolutePath()
    private val lock = Object()

    private val usersPath: Path get() = dataDir.resolve("users.json")
    private val configPath: Path get() = dataDir.resolve("config.json")
    private fun ordersDir(): Path = dataDir.resolve("orders")
    private fun dayPath(date: LocalDate): Path = ordersDir().resolve("$date.json")
    private fun evalsDir(): Path = dataDir.resolve("evaluations")
    private fun evalPath(date: LocalDate): Path = evalsDir().resolve("$date.json")

    init {
        // 构造时自举（幂等）：Spring 与测试环境行为一致
        bootstrap()
    }

    fun dataDirectory(): Path = dataDir

    /** 启动校验：建目录、校验/种子 users.json、确保 config.json 存在 */
    fun bootstrap() {
        Files.createDirectories(dataDir)
        Files.createDirectories(ordersDir())
        Files.createDirectories(evalsDir())
        withLock {
            ensureUsers()
            if (!Files.exists(configPath)) {
                atomicWrite(configPath, mapper.writeValueAsString(AppConfigJson()))
            }
        }
    }

    /** 服务层做读改写时使用同一把锁，保证复合操作串行 */
    fun <T> withLock(block: () -> T): T = synchronized(lock) { block() }

    // ---------- users.json ----------

    private fun ensureUsers() {
        if (!Files.exists(usersPath)) {
            seedUsers()
            return
        }
        try {
            mapper.readValue(usersPath.toFile(), UsersFile::class.java)
        } catch (e: Exception) {
            val backup = dataDir.resolve("users.json.corrupt-${System.currentTimeMillis()}")
            runCatching { Files.move(usersPath, backup) }
            log.warn("users.json 无法解析，已备份为 {} 并重新种子初始化", backup.fileName, e)
            seedUsers()
        }
    }

    private fun seedUsers() {
        val now = TimeUtil.format(ZonedDateTime.now(TimeUtil.SHANGHAI))
        val encoder = BCryptPasswordEncoder()
        fun make(login: String, display: String, pwd: String, role: String) = User(
            loginName = login, displayName = display, passwordHash = encoder.encode(pwd),
            passwordEnc = vault?.encrypt(pwd),
            pwdVer = 1, role = role, createdAt = now, updatedAt = now,
        )
        val users = mutableListOf(
            make("admin", "管理员", "admin123", "ADMIN"),
            make("zhangsan", "张三", "123456", "USER"),
            make("lisi", "李四", "123456", "USER"),
            make("wangwu", "王五", "123456", "USER"),
            make("zhaoliu", "赵六", "123456", "USER"),
            make("sunqi", "孙七", "123456", "USER"),
        )
        atomicWrite(usersPath, mapper.writeValueAsString(UsersFile(users)))
        log.info("已完成用户种子初始化：admin/admin123 与 5 个演示账号（zhangsan 等，密码 123456）")
    }

    fun readUsers(): UsersFile = mapper.readValue(usersPath.toFile(), UsersFile::class.java)

    /** 认证热路径用：损坏时按空用户表处理（登录必然失败），不抛异常 */
    fun readUsersSafe(): UsersFile =
        try {
            readUsers()
        } catch (e: Exception) {
            log.warn("users.json 读取失败，本次按空处理", e)
            UsersFile()
        }

    fun writeUsers(usersFile: UsersFile) = withLock {
        atomicWrite(usersPath, mapper.writeValueAsString(usersFile))
    }

    // ---------- orders/YYYY-MM-DD.json ----------

    fun readDay(date: LocalDate): DayOrders? {
        val path = dayPath(date)
        if (!Files.exists(path)) return null
        return try {
            mapper.readValue(path.toFile(), DayOrders::class.java)
        } catch (e: Exception) {
            log.warn("{} 无法解析，按空处理（下次保存会自愈）", path.fileName, e)
            null
        }
    }

    fun writeDay(day: DayOrders) = withLock {
        atomicWrite(dayPath(LocalDate.parse(day.date)), mapper.writeValueAsString(day))
    }

    // ---------- evaluations/YYYY-MM-DD.json（D-012） ----------

    fun readEvaluations(date: LocalDate): DayEvaluations? {
        val path = evalPath(date)
        if (!Files.exists(path)) return null
        return try {
            mapper.readValue(path.toFile(), DayEvaluations::class.java)
        } catch (e: Exception) {
            log.warn("{} 无法解析，按空处理（下次保存会自愈）", path.fileName, e)
            null
        }
    }

    fun writeEvaluations(day: DayEvaluations) = withLock {
        atomicWrite(evalPath(LocalDate.parse(day.date)), mapper.writeValueAsString(day))
    }

    // ---------- config.json ----------

    fun readConfig(): AppConfigJson =
        if (!Files.exists(configPath)) AppConfigJson()
        else runCatching { mapper.readValue(configPath.toFile(), AppConfigJson::class.java) }
            .getOrDefault(AppConfigJson())

    fun writeConfig(config: AppConfigJson) = withLock {
        atomicWrite(configPath, mapper.writeValueAsString(config))
    }

    // ---------- 底层 ----------

    private fun atomicWrite(path: Path, content: String) {
        val tmp = path.resolveSibling("${path.fileName}.tmp-${UUID.randomUUID()}")
        Files.writeString(tmp, content, StandardCharsets.UTF_8)
        try {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

/** 便于服务层构造记录 */
fun newOrder(loginName: String, displayName: String, spicy: Boolean, at: String) =
    OrderRecord(loginName = loginName, displayName = displayName, spicy = spicy, orderedAt = at)
