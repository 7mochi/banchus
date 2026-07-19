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
import pe.nanamochi.banchus.redis.stream.MessageInfo
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule

@Configuration
class RedisConfig {
    private fun <T : Any> createKotlinSerializer(type: Class<T>): RedisSerializer<T> {
        val mapper = jsonMapper { addModule(kotlinModule()) }
        return JacksonJsonRedisSerializer(mapper, type)
    }

    private fun <T : Any> createTemplate(
        factory: RedisConnectionFactory,
        serializer: RedisSerializer<T>,
    ): RedisTemplate<String, T> =
        RedisTemplate<String, T>().apply {
            connectionFactory = factory
            keySerializer = RedisSerializer.string()
            valueSerializer = serializer
            hashKeySerializer = RedisSerializer.string()
            hashValueSerializer = serializer
        }

    @Bean
    fun sessionSerializer(): RedisSerializer<Session> = createKotlinSerializer(Session::class.java)

    @Bean
    fun messageInfoSerializer(): RedisSerializer<MessageInfo> =
        createKotlinSerializer(MessageInfo::class.java)

    @Bean
    @Primary
    fun sessionRedisTemplate(
        factory: RedisConnectionFactory,
        sessionSerializer: RedisSerializer<Session>,
    ): RedisTemplate<String, Session> = createTemplate(factory, sessionSerializer)

    @Bean
    fun sessionIdentityRedisTemplate(
        factory: RedisConnectionFactory
    ): RedisTemplate<String, SessionIdentity> =
        createTemplate(factory, createKotlinSerializer(SessionIdentity::class.java))

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
        createTemplate(factory, createKotlinSerializer(Presence::class.java))

    @Bean
    fun multiplayerMatchRedisTemplate(
        factory: RedisConnectionFactory
    ): RedisTemplate<String, MultiplayerMatch> =
        createTemplate(factory, createKotlinSerializer(MultiplayerMatch::class.java))

    @Bean
    fun byteArrayRedisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, ByteArray> =
        RedisTemplate<String, ByteArray>().apply {
            connectionFactory = factory
            keySerializer = RedisSerializer.string()
            valueSerializer = RedisSerializer.byteArray()
            hashKeySerializer = RedisSerializer.string()
            hashValueSerializer = RedisSerializer.byteArray()
        }

    @Bean
    fun multiplayerMatchSlotRedisTemplate(
        factory: RedisConnectionFactory
    ): RedisTemplate<String, MultiplayerMatchSlot> =
        createTemplate(factory, createKotlinSerializer(MultiplayerMatchSlot::class.java))
}
