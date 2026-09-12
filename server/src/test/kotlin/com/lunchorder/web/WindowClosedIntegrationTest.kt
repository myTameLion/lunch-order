package com.lunchorder.web

import com.lunchorder.LunchServerApplication
import com.lunchorder.util.TimeUtil
import com.lunchorder.web.dto.ErrorResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.nio.file.Path
import java.time.Clock
import java.time.Instant

/** 固定 2026-09-07 12:00（+08:00，窗口外） */
@TestConfiguration
class FixedClosedClockConfig {
    @Bean
    @Primary
    fun closedClock(): Clock = Clock.fixed(Instant.parse("2026-09-07T04:00:00Z"), TimeUtil.SHANGHAI)
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(FixedClosedClockConfig::class)
class WindowClosedIntegrationTest {

    @Autowired
    lateinit var rest: TestRestTemplate

    companion object {
        @TempDir
        @JvmStatic
        lateinit var temp: Path

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("lunch.data-dir") { temp.toString() }
            registry.add("lunch.jwt-secret") { "fedcba9876543210fedcba9876543210" }
        }
    }

    private inline fun <reified T> call(method: HttpMethod, url: String, token: String? = null, body: Any? = null): ResponseEntity<T> {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            token?.let { setBearerAuth(it) }
        }
        return rest.exchange(url, method, HttpEntity(body, headers), T::class.java)
    }

    private fun login(user: String, pwd: String): String {
        val resp = call<Map<*, *>>(HttpMethod.POST, "/api/auth/login", body = mapOf("loginName" to user, "password" to pwd))
        assertEquals(HttpStatus.OK, resp.statusCode)
        return resp.body!!["token"] as String
    }

    @Test
    fun `窗口外登记与取消均 409-2001，管理员不受限`() {
        val user = login("zhangsan", "123456")
        val admin = login("admin", "admin123")

        val put = call<ErrorResponse>(HttpMethod.PUT, "/api/orders/today", token = user, body = mapOf("spicy" to true))
        assertEquals(HttpStatus.CONFLICT, put.statusCode)
        assertEquals(2001, put.body!!.code)

        val del = call<ErrorResponse>(HttpMethod.DELETE, "/api/orders/today", token = user)
        assertEquals(HttpStatus.CONFLICT, del.statusCode)
        assertEquals(2001, del.body!!.code)

        val adminPut = call<Map<*, *>>(HttpMethod.PUT, "/api/orders/today", token = admin, body = mapOf("spicy" to true))
        assertEquals(HttpStatus.OK, adminPut.statusCode)
        assertEquals(false, adminPut.body!!["updated"])
    }
}
