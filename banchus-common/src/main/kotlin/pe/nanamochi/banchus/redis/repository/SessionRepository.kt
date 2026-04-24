package pe.nanamochi.banchus.redis.repository

import java.time.Instant
import java.util.UUID
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.redis.entity.Session

private const val SESSIONS_KEY = "banchus:sessions"

@Repository
class SessionRepository(
    private val redisTemplate: RedisTemplate<String, Session>,
    private val stringRedisTemplate: RedisTemplate<String, String>,
) {
    fun create(session: Session): Session {
        session.updatedAt = Instant.now()

        val userIdKey = makeIdKey(session.userId)
        val usernameKey = makeUsernameKey(session.username)

        redisTemplate
            .opsForHash<String, Session>()
            .put(SESSIONS_KEY, session.sessionId.toString(), session)
        stringRedisTemplate.opsForSet().add(userIdKey, session.sessionId.toString())
        stringRedisTemplate.opsForSet().add(usernameKey, session.sessionId.toString())

        return session
    }

    fun update(session: Session): Session {
        session.updatedAt = Instant.now()
        redisTemplate
            .opsForHash<String, Session>()
            .put(SESSIONS_KEY, session.sessionId.toString(), session)
        return session
    }

    fun delete(sessionId: UUID, userId: Int, username: String): Long {
        val userIdKey = makeIdKey(userId)
        val usernameKey = makeUsernameKey(username)

        redisTemplate.opsForHash<String, Session>().delete(SESSIONS_KEY, sessionId.toString())
        stringRedisTemplate.opsForSet().remove(userIdKey, sessionId.toString())
        stringRedisTemplate.opsForSet().remove(usernameKey, sessionId.toString())

        return stringRedisTemplate.opsForSet().size(userIdKey) ?: 0L
    }

    fun setPrivateDms(session: Session, privateDms: Boolean): Session {
        session.privateDms = privateDms
        return update(session)
    }

    fun findById(sessionId: UUID): Session? =
        redisTemplate.opsForHash<String, Session>().get(SESSIONS_KEY, sessionId.toString())

    fun fetchAll(): List<Session> = redisTemplate.opsForHash<String, Session>().values(SESSIONS_KEY)

    fun fetchMany(sessionIds: List<UUID>): List<Session> {
        if (sessionIds.isEmpty()) return emptyList()
        val keys = sessionIds.map { it.toString() }
        return redisTemplate
            .opsForHash<String, Session>()
            .multiGet(SESSIONS_KEY, keys)
            .filterNotNull()
    }

    fun fetchUserSessionCount(userId: Int): Int {
        val userIdKey = makeIdKey(userId)
        return stringRedisTemplate.opsForSet().size(userIdKey)?.toInt() ?: 0
    }

    fun fetchByUserId(userId: Int): List<Session> {
        val sessionIds =
            stringRedisTemplate.opsForSet().members(makeIdKey(userId))?.map { UUID.fromString(it) }
                ?: return emptyList()
        return fetchMany(sessionIds)
    }

    fun fetchByUsername(username: String): List<Session> {
        val sessionIds =
            stringRedisTemplate.opsForSet().members(makeUsernameKey(username))?.map {
                UUID.fromString(it)
            } ?: return emptyList()
        return fetchMany(sessionIds)
    }

    fun isOnline(userId: Int): Boolean = stringRedisTemplate.hasKey(makeIdKey(userId))

    fun count(): Long = redisTemplate.opsForHash<String, Session>().size(SESSIONS_KEY)

    private fun makeIdKey(userId: Int): String = "banchus:sessions:user_ids:$userId"

    private fun makeUsernameKey(username: String): String {
        val safeUsername = username.lowercase().replace(" ", "_")
        return "banchus:sessions:usernames:$safeUsername"
    }
}
