package pe.nanamochi.banchus.redis.repository

import java.util.UUID
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository

@Repository
class ChannelMembersRepository(private val redisTemplate: RedisTemplate<String, UUID>) {

    fun add(channelId: UUID, sessionId: UUID): UUID =
        sessionId.also { redisTemplate.opsForSet().add(makeKey(channelId), it) }

    fun remove(channelId: UUID, sessionId: UUID): UUID? =
        redisTemplate
            .opsForSet()
            .remove(makeKey(channelId), sessionId.toString())
            .takeIf { it == 1L }
            ?.let { sessionId }

    fun getMembers(channelId: UUID): Set<UUID> {
        return redisTemplate.opsForSet().members(makeKey(channelId)) ?: emptySet()
    }

    fun getMemberCount(channelId: UUID): Int =
        redisTemplate.opsForSet().size(makeKey(channelId))?.toInt() ?: 0

    private fun makeKey(channelId: UUID): String = "server:channel-members:$channelId"
}
