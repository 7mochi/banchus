package pe.nanamochi.banchus.config

import java.util.UUID
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericToStringSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import pe.nanamochi.banchus.redis.entity.MultiplayerMatch
import pe.nanamochi.banchus.redis.entity.PacketBundle

@Configuration
class RedisConfig {

    @Bean
    fun multiplayerMatchRedisTemplate(
        factory: RedisConnectionFactory
    ): RedisTemplate<String, MultiplayerMatch> =
        RedisTemplate<String, MultiplayerMatch>().apply {
            connectionFactory = factory
            keySerializer = RedisSerializer.string()
            valueSerializer = RedisSerializer.json()
        }

    @Bean
    fun packetBundleRedisTemplate(
        factory: RedisConnectionFactory
    ): RedisTemplate<String, PacketBundle> =
        RedisTemplate<String, PacketBundle>().apply {
            connectionFactory = factory
            keySerializer = RedisSerializer.string()
            valueSerializer = RedisSerializer.json()
        }

    @Bean
    fun uuidRedisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, UUID> =
        RedisTemplate<String, UUID>().apply {
            connectionFactory = factory
            keySerializer = RedisSerializer.string()
            valueSerializer = GenericToStringSerializer(UUID::class.java)
        }
}
