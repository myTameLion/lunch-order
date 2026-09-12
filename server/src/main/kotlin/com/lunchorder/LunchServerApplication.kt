package com.lunchorder

import com.lunchorder.store.DataStore
import com.lunchorder.util.TimeUtil.SHANGHAI
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import java.net.Inet4Address
import java.net.NetworkInterface
import java.time.Clock

@SpringBootApplication
@EnableConfigurationProperties(LunchProperties::class)
class LunchServerApplication {

    private val log = LoggerFactory.getLogger(LunchServerApplication::class.java)

    /** 启动完成后打印后台可访问地址（localhost + 局域网 IPv4） */
    @EventListener(ApplicationReadyEvent::class)
    fun printAccessUrls(event: ApplicationReadyEvent) {
        val env = event.applicationContext.environment
        val port = env.getProperty("local.server.port") ?: env.getProperty("server.port") ?: "8080"
        val dataDir = event.applicationContext.getBean(DataStore::class.java).dataDirectory()

        val ips = runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() }
                .filterIsInstance<Inet4Address>()
                .filter { !it.isLoopbackAddress }
                .mapNotNull { it.hostAddress }
                .toList()
        }.getOrDefault(emptyList())

        val lines = buildList {
            add("内网点餐系统已启动")
            add("  本机访问:   http://localhost:$port/")
            ips.forEach { ip ->
                add("  局域网访问: http://$ip:$port/  （员工端，同一地址加 /admin/ 为管理中台）")
            }
            if (ips.isEmpty()) add("  （未检测到局域网 IPv4，仅本机可访问）")
            add("  数据目录:   $dataDir")
        }
        log.info(
            lines.joinToString(
                "\n",
                prefix = "\n┌───────────────────────────────────────────────\n",
                postfix = "\n└───────────────────────────────────────────────",
            ),
        )
    }
}

fun main(args: Array<String>) {
    runApplication<LunchServerApplication>(*args)
}

/** 业务时间统一由可注入的 Clock 产生（D-007），生产固定 Asia/Shanghai，测试可注入固定时钟 */
@Configuration
class ClockConfig {
    @Bean
    fun clock(): Clock = Clock.system(SHANGHAI)
}
