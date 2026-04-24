package pe.nanamochi.banchus.redis.repository

import java.util.UUID
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.redis.entity.SessionIdentity

private const val SPECTATING_KEY = "banchus:sessions:spectating"

@Repository
class SpectatorRepository(
    private val redisTemplate: RedisTemplate<String, UUID>,
    private val redisIdentityTemplate: RedisTemplate<String, SessionIdentity>,
) {
    fun fetchSpectating(sessionId: UUID): UUID? {
        return redisTemplate.opsForHash<String, UUID>().get(SPECTATING_KEY, sessionId.toString())
    }

    fun removeSpectating(sessionId: UUID) {
        redisTemplate.opsForHash<String, UUID>().delete(SPECTATING_KEY, sessionId.toString())
    }

    fun addMember(hostSessionId: UUID, member: SessionIdentity): Long {
        val key = makeKey(hostSessionId)
        redisTemplate
            .opsForHash<String, UUID>()
            .put(SPECTATING_KEY, member.sessionId.toString(), hostSessionId)
        redisIdentityTemplate.opsForSet().add(key, member)
        return redisIdentityTemplate.opsForSet().size(key) ?: 0L
    }

    fun removeMember(hostSessionId: UUID, member: SessionIdentity): Long {
        val key = makeKey(hostSessionId)
        redisTemplate.opsForHash<String, UUID>().delete(SPECTATING_KEY, member.sessionId.toString())
        redisIdentityTemplate.opsForSet().remove(key, member)
        return redisIdentityTemplate.opsForSet().size(key) ?: 0L
    }

    fun removeMembers(hostSessionId: UUID) {
        redisIdentityTemplate.delete(makeKey(hostSessionId))
    }

    fun fetchAllMembers(hostSessionId: UUID): Set<SessionIdentity> {
        return redisIdentityTemplate.opsForSet().members(makeKey(hostSessionId)) ?: emptySet()
    }

    private fun makeKey(hostSessionId: UUID) = "banchus:spectator-$hostSessionId"
}
