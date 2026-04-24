package pe.nanamochi.banchus.config

import java.util.UUID
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import pe.nanamochi.banchus.redis.entity.MultiplayerMatch
import pe.nanamochi.banchus.redis.entity.MultiplayerMatchSlot
import pe.nanamochi.banchus.redis.entity.Presence
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.redis.entity.SessionIdentity
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule

@Configuration
class RedisConfig {
    private fun <T : Any> createKotlinSerializer(type: Class<T>): RedisSerializer<T> {
        val mapper = jsonMapper { addModule(kotlinModule()) }
        return JacksonJsonRedisSerializer(mapper, type)
    }

    @Bean
    @Primary
    fun sessionRedisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, Session> =
        RedisTemplate<String, Session>().apply {
            val serializer = createKotlinSerializer(Session::class.java)
            connectionFactory = factory
            keySerializer = RedisSerializer.string()
            valueSerializer = serializer
            hashKeySerializer = RedisSerializer.string()
            hashValueSerializer = serializer
        }

    @Bean
    @Primary
    fun sessionIdentityRedisTemplate(
        factory: RedisConnectionFactory
    ): RedisTemplate<String, SessionIdentity> =
        RedisTemplate<String, SessionIdentity>().apply {
            val serializer = createKotlinSerializer(SessionIdentity::class.java)
            connectionFactory = factory
            keySerializer = RedisSerializer.string()
            valueSerializer = serializer
            hashKeySerializer = RedisSerializer.string()
            hashValueSerializer = serializer
        }

    @Bean
    fun uuidRedisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, UUID> =
        RedisTemplate<String, UUID>().apply {
            connectionFactory = factory
            keySerializer = RedisSerializer.string()
            valueSerializer = RedisSerializer.java()
            hashKeySerializer = RedisSerializer.string()
            hashValueSerializer = RedisSerializer.java()
        }

    @Bean
    fun presenceRedisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, Presence> =
        RedisTemplate<String, Presence>().apply {
            val serializer = createKotlinSerializer(Presence::class.java)
            connectionFactory = factory
            keySerializer = RedisSerializer.string()
            valueSerializer = serializer
            hashKeySerializer = RedisSerializer.string()
            hashValueSerializer = serializer
        }

    @Bean
    fun multiplayerMatchRedisTemplate(
        factory: RedisConnectionFactory
    ): RedisTemplate<String, MultiplayerMatch> =
        RedisTemplate<String, MultiplayerMatch>().apply {
            val serializer = createKotlinSerializer(MultiplayerMatch::class.java)
            connectionFactory = factory
            keySerializer = RedisSerializer.string()
            valueSerializer = serializer
            hashKeySerializer = RedisSerializer.string()
            hashValueSerializer = serializer
        }

    @Bean
    fun multiplayerMatchSlotRedisTemplate(
        factory: RedisConnectionFactory
    ): RedisTemplate<String, MultiplayerMatchSlot> =
        RedisTemplate<String, MultiplayerMatchSlot>().apply {
            val serializer = createKotlinSerializer(MultiplayerMatchSlot::class.java)
            connectionFactory = factory
            keySerializer = RedisSerializer.string()
            valueSerializer = serializer
            hashKeySerializer = RedisSerializer.string()
            hashValueSerializer = serializer
        }
}
