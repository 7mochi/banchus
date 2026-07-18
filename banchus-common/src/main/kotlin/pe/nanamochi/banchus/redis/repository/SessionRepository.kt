package pe.nanamochi.banchus.redis.repository

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID
import kotlin.Suppress
import org.springframework.data.redis.core.RedisOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.SessionCallback
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.redis.entity.Session

private const val SESSIONS_KEY = "banchus:sessions"

@Repository
class SessionRepository(
    private val redisTemplate: RedisTemplate<String, Session>,
    private val stringRedisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    fun create(session: Session): Session {
        session.updatedAt = Instant.now()
        val sessionJson = objectMapper.writeValueAsString(session)

        stringRedisTemplate.execute(
            object : SessionCallback<List<*>> {
                @Suppress("UNCHECKED_CAST")
                override fun <K : Any, V : Any> execute(
                    operations: RedisOperations<K, V>
                ): List<*> {
                    val ops = operations as RedisOperations<String, String>
                    ops.multi()
                    ops.opsForHash<String, String>()
                        .put(SESSIONS_KEY, session.sessionId.toString(), sessionJson)
                    ops.opsForSet().add(makeIdKey(session.userId), session.sessionId.toString())
                    ops.opsForSet()
                        .add(makeUsernameKey(session.username), session.sessionId.toString())
                    return ops.exec()
                }
            }
        )

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

        val results =
            stringRedisTemplate.execute(
                object : SessionCallback<List<*>> {
                    @Suppress("UNCHECKED_CAST")
                    override fun <K : Any, V : Any> execute(
                        operations: RedisOperations<K, V>
                    ): List<*> {
                        val ops = operations as RedisOperations<String, String>
                        ops.multi()
                        ops.opsForHash<String, String>().delete(SESSIONS_KEY, sessionId.toString())
                        ops.opsForSet().remove(userIdKey, sessionId.toString())
                        ops.opsForSet().remove(usernameKey, sessionId.toString())
                        ops.opsForSet().size(userIdKey)
                        return ops.exec()
                    }
                }
            )

        return (results?.lastOrNull() as? Long) ?: 0L
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
