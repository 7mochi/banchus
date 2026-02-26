package pe.nanamochi.banchus.redis.repository

import java.util.UUID
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository

@Repository
class SpectatorRepository(private val redisTemplate: RedisTemplate<String, UUID>) {

    fun add(hostSessionId: UUID, sessionId: UUID): UUID =
        sessionId.also { redisTemplate.opsForSet().add(makeKey(hostSessionId), sessionId) }

    fun remove(hostSessionId: UUID, sessionId: UUID): UUID? =
        redisTemplate
            .opsForSet()
            .remove(makeKey(hostSessionId), sessionId)
            .takeIf { it == 1L }
            ?.let { sessionId }

    fun getMembers(hostSessionId: UUID): Set<UUID> {
        return redisTemplate.opsForSet().members(makeKey(hostSessionId)) ?: emptySet()
    }

    private fun makeKey(hostSessionId: UUID): String = "server:spectators:$hostSessionId"
}
