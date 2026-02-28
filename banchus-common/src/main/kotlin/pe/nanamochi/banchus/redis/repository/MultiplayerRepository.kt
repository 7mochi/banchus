package pe.nanamochi.banchus.redis.repository

import java.time.Instant
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.redis.entity.MultiplayerMatch
import pe.nanamochi.banchus.redis.entity.MultiplayerSlot

@Repository
class MultiplayerRepository(
    private val redisTemplate: RedisTemplate<String, MultiplayerMatch>,
    private val stringRedisTemplate: RedisTemplate<String, String>,
) {
    fun nextMatchId(): Long = stringRedisTemplate.opsForValue().increment("server:last_match_id")

    fun create(match: MultiplayerMatch): MultiplayerMatch? =
        match
            .also {
                it.updatedAt = Instant.now()
                redisTemplate.opsForValue().set(makeKey(it.matchId), it)
            }
            .let { findById(it.matchId) }

    fun update(match: MultiplayerMatch): MultiplayerMatch? = create(match)

    fun findById(matchId: Int): MultiplayerMatch? =
        redisTemplate.opsForValue().get(makeKey(matchId))

    fun fetchAll(): List<MultiplayerMatch> {
        val pattern = "server:matches:*"

        return redisTemplate.execute { connection ->
            val options = ScanOptions.scanOptions().match(pattern).build()
            val matches = mutableListOf<MultiplayerMatch>()

            connection.keyCommands().scan(options).use { cursor ->
                cursor.forEach { keyBytes ->
                    connection.stringCommands().get(keyBytes)?.let { valueBytes ->
                        (redisTemplate.valueSerializer?.deserialize(valueBytes)
                                as? MultiplayerMatch)
                            ?.let { matches.add(it) }
                    }
                }
            }
            matches
        } ?: emptyList()
    }

    fun fetchAllSlots(matchId: Int): List<MultiplayerSlot> =
        findById(matchId)?.slots?.sortedBy { slot -> slot.slotId } ?: emptyList()

    fun delete(matchId: Int) {
        redisTemplate.delete(makeKey(matchId))
    }

    private fun makeKey(matchId: Int): String = "server:matches:$matchId"
}
