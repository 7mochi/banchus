package pe.nanamochi.banchus.core.repository

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.core.entity.Presence

private const val PRESENCE_KEY = "banchus:presences"

@Repository
class PresenceRepository(private val redisTemplate: RedisTemplate<String, Presence>) {
    fun create(presence: Presence): Presence {
        if (presence.userId != Presence.BOT_ID) {
            redisTemplate
                .opsForHash<String, Presence>()
                .put(PRESENCE_KEY, presence.userId.toString(), presence)
        }
        return presence
    }

    fun fetchOne(userId: Int): Presence? {
        if (userId == Presence.BOT_ID) return Presence.botPresence()

        return redisTemplate.opsForHash<String, Presence>().get(PRESENCE_KEY, userId.toString())
    }

    fun fetchMultiple(userIds: List<Int>): List<Presence?> {
        val keys = userIds.map { it.toString() }
        val results = redisTemplate.opsForHash<String, Presence>().multiGet(PRESENCE_KEY, keys)

        return userIds.indices.map { i ->
            if (userIds[i] == Presence.BOT_ID) {
                Presence.botPresence()
            } else {
                results[i]
            }
        }
    }

    fun fetchUserIds(): List<Int> {
        val ids =
            redisTemplate
                .opsForHash<String, Presence>()
                .keys(PRESENCE_KEY)
                .map { it.toInt() }
                .toMutableList()

        if (!ids.contains(Presence.BOT_ID)) {
            ids.add(Presence.BOT_ID)
        }
        return ids
    }

    fun fetchAll(): List<Presence> {
        val presences =
            redisTemplate.opsForHash<String, Presence>().values(PRESENCE_KEY).toMutableList()
        presences.add(Presence.botPresence())
        return presences
    }

    fun update(presence: Presence): Presence {
        if (presence.userId == Presence.BOT_ID) return Presence.botPresence()

        redisTemplate
            .opsForHash<String, Presence>()
            .put(PRESENCE_KEY, presence.userId.toString(), presence)
        return presence
    }

    fun delete(userId: Int) {
        if (userId == Presence.BOT_ID) {
            // Aquí podrías usar un logger: "Tried to delete bot presence, ignoring."
            return
        }
        redisTemplate.opsForHash<String, Presence>().delete(PRESENCE_KEY, userId.toString())
    }
}
