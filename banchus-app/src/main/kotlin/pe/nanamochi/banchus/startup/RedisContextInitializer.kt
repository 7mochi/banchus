package pe.nanamochi.banchus.startup

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory

class RedisContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun initialize(context: ConfigurableApplicationContext) {
        val env = context.environment

        val host = env.getProperty("spring.data.redis.host") ?: "localhost"
        val port = env.getProperty("spring.data.redis.port")?.toInt() ?: 6379

        val factory = LettuceConnectionFactory(host, port).apply { afterPropertiesSet() }

        runCatching { factory.connection.use { it.ping() } }
            .onSuccess { log.info("Successfully connected to Redis server at $host:$port") }
            .onFailure { e ->
                log.error("Could not connect to Redis at $host:$port. Shutting down.")
                throw IllegalStateException("Redis connection is mandatory for Banchus to start", e)
            }

        factory.destroy()
    }
}
