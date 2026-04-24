package pe.nanamochi.banchus.redis.repository

import java.time.Instant
import java.util.UUID
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.Match
import pe.nanamochi.banchus.database.repository.MatchRepository
import pe.nanamochi.banchus.domain.enums.SlotStatus
import pe.nanamochi.banchus.redis.entity.MultiplayerMatch
import pe.nanamochi.banchus.redis.entity.MultiplayerMatchSlot
import pe.nanamochi.banchus.redis.entity.SessionIdentity

enum class TimerType {
    REGULAR,
    MATCH_START,
}

private const val MATCHES_KEY = "banchus:multiplayer"
private const val SESSIONS_MATCHES_KEY = "banchus:sessions:multiplayer"
private const val MULTIPLAYER_MAX_SIZE = 16

@Repository
class MultiplayerRepository(
    private val matchTemplate: RedisTemplate<String, MultiplayerMatch>,
    private val slotTemplate: RedisTemplate<String, MultiplayerMatchSlot>,
    private val stringRedisTemplate: RedisTemplate<String, String>,
    private val matchRepository: MatchRepository,
) {
    fun create(
        host: SessionIdentity,
        name: String,
        password: String?,
        beatmapName: String,
        beatmapMd5: String,
        beatmapId: Int,
        mode: UByte,
        maxPlayerCount: Int,
    ): Pair<MultiplayerMatch, List<MultiplayerMatchSlot>> {
        val mpMatch =
            MultiplayerMatch(
                matchId = 0,
                name = name,
                password = password,
                hostUserId = host.userId,
                beatmapName = beatmapName,
                beatmapMd5 = beatmapMd5,
                beatmapId = beatmapId,
                mode = mode,
            )

        val private = password.isNullOrBlank() || password.isEmpty()
        val matchId = matchRepository.save(Match(name = name, private = private)).id
        mpMatch.matchId = matchId

        val slots =
            List(MULTIPLAYER_MAX_SIZE) { i ->
                MultiplayerMatchSlot().apply {
                    when {
                        i == 0 -> prepare(host)
                        i >= maxPlayerCount -> status = SlotStatus.LOCKED
                        else -> status = SlotStatus.OPEN
                    }
                }
            }

        val slotsKey = makeSlotsKey(matchId)

        matchTemplate
            .opsForHash<String, MultiplayerMatch>()
            .put(MATCHES_KEY, matchId.toString(), mpMatch)

        slots.forEachIndexed { index, slot ->
            slotTemplate
                .opsForHash<String, MultiplayerMatchSlot>()
                .put(slotsKey, index.toString(), slot)
        }
        stringRedisTemplate
            .opsForHash<String, String>()
            .put(SESSIONS_MATCHES_KEY, host.sessionId.toString(), matchId.toString())

        return Pair(mpMatch, slots)
    }

    fun delete(matchId: Long) {
        val slotsKey = makeSlotsKey(matchId)
        val refereesKey = makeRefereesKey(matchId)
        val timerKey = makeTimerKey(matchId, TimerType.REGULAR)
        val startTimerKey = makeTimerKey(matchId, TimerType.MATCH_START)

        matchTemplate.opsForHash<String, MultiplayerMatch>().delete(MATCHES_KEY, matchId.toString())
        stringRedisTemplate.delete(listOf(slotsKey, refereesKey, timerKey, startTimerKey))

        matchRepository.findById(matchId).ifPresent { match ->
            match.endTime = Instant.now()
            matchRepository.save(match)
        }
    }

    fun update(mpMatch: MultiplayerMatch, updatePersistent: Boolean): MultiplayerMatch {
        matchTemplate
            .opsForHash<String, MultiplayerMatch>()
            .put(MATCHES_KEY, mpMatch.matchId.toString(), mpMatch)

        if (updatePersistent) {
            val isPrivate = !mpMatch.password.isNullOrBlank()
            matchRepository.findById(mpMatch.matchId).ifPresent { match ->
                match.name = mpMatch.name
                match.private = isPrivate
                matchRepository.save(match)
            }
        }

        return mpMatch
    }

    fun join(identity: SessionIdentity, matchId: Long): List<MultiplayerMatchSlot>? {
        val slots = fetchAllSlots(matchId).toMutableList()
        val emptyIndex = slots.indexOfFirst { it.status == SlotStatus.OPEN }

        if (emptyIndex == -1) return null

        slots[emptyIndex].prepare(identity)

        stringRedisTemplate
            .opsForHash<String, String>()
            .put(SESSIONS_MATCHES_KEY, identity.sessionId.toString(), matchId.toString())
        slotTemplate
            .opsForHash<String, MultiplayerMatchSlot>()
            .put(makeSlotsKey(matchId), emptyIndex.toString(), slots[emptyIndex])

        return slots
    }

    fun leave(sessionId: UUID, matchId: Long): Pair<Int, List<MultiplayerMatchSlot>>? {
        val slots = fetchAllSlots(matchId).toMutableList()
        val slotIndex = slots.indexOfFirst { it.user?.sessionId == sessionId }

        if (slotIndex == -1) return null

        slots[slotIndex].clear()
        val userCount = slots.count { it.user != null }
        val slotsKey = makeSlotsKey(matchId)

        stringRedisTemplate
            .opsForHash<String, String>()
            .delete(SESSIONS_MATCHES_KEY, sessionId.toString())

        if (userCount == 0) {
            matchTemplate
                .opsForHash<String, MultiplayerMatch>()
                .delete(MATCHES_KEY, matchId.toString())
            stringRedisTemplate.delete(slotsKey)
            stringRedisTemplate.delete(makeRefereesKey(matchId))
        } else {
            slotTemplate
                .opsForHash<String, MultiplayerMatchSlot>()
                .put(slotsKey, slotIndex.toString(), slots[slotIndex])
        }

        return Pair(userCount, slots)
    }

    fun fetchSessionMatchId(sessionId: UUID): Long? =
        stringRedisTemplate
            .opsForHash<String, String>()
            .get(SESSIONS_MATCHES_KEY, sessionId.toString())
            ?.toLong()

    fun fetchOne(matchId: Long): MultiplayerMatch? =
        matchTemplate.opsForHash<String, MultiplayerMatch>().get(MATCHES_KEY, matchId.toString())

    fun fetchAll(): List<MultiplayerMatch> =
        matchTemplate.opsForHash<String, MultiplayerMatch>().values(MATCHES_KEY)

    fun fetchSlot(matchId: Long, slotId: Int): MultiplayerMatchSlot? {
        return slotTemplate
            .opsForHash<String, MultiplayerMatchSlot>()
            .get(makeSlotsKey(matchId), slotId.toString())
    }

    fun fetchAllSlots(matchId: Long): List<MultiplayerMatchSlot> {
        val fields = (0 until MULTIPLAYER_MAX_SIZE).map { it.toString() }
        return slotTemplate
            .opsForHash<String, MultiplayerMatchSlot>()
            .multiGet(makeSlotsKey(matchId), fields)
            .filterNotNull()
    }

    fun updateSlot(matchId: Long, slotId: Int, slot: MultiplayerMatchSlot) {
        slotTemplate
            .opsForHash<String, MultiplayerMatchSlot>()
            .put(makeSlotsKey(matchId), slotId.toString(), slot)
    }

    fun updateSlots(matchId: Long, slots: List<Pair<Int, MultiplayerMatchSlot>>) {
        if (slots.isEmpty()) return

        val updates = slots.associate { (id, slot) -> id.toString() to slot }

        slotTemplate
            .opsForHash<String, MultiplayerMatchSlot>()
            .putAll(makeSlotsKey(matchId), updates)
    }

    fun updateAllSlots(matchId: Long, slots: List<MultiplayerMatchSlot>) {
        val slotsKey = makeSlotsKey(matchId)
        val map = slots.mapIndexed { index, slot -> index.toString() to slot }.toMap()
        slotTemplate.opsForHash<String, MultiplayerMatchSlot>().putAll(slotsKey, map)
    }

    fun addReferee(matchId: Long, userId: Int) {
        stringRedisTemplate.opsForSet().add(makeRefereesKey(matchId), userId.toString())
    }

    fun removeReferee(matchId: Long, userId: Int) {
        stringRedisTemplate.opsForSet().remove(makeRefereesKey(matchId), userId.toString())
    }

    fun getReferees(matchId: Long): List<Int> =
        stringRedisTemplate.opsForSet().members(makeRefereesKey(matchId))?.map { it.toInt() }
            ?: emptyList()

    fun isReferee(matchId: Long, userId: Int): Boolean {
        return stringRedisTemplate.opsForSet().isMember(makeRefereesKey(matchId), userId.toString())
            ?: false
    }

    fun clearReferees(matchId: Long) {
        stringRedisTemplate.delete(makeRefereesKey(matchId))
    }

    fun getTimer(matchId: Long, type: TimerType): Long? {
        return stringRedisTemplate.opsForValue().get(makeTimerKey(matchId, type))?.toLong()
    }

    fun setTimer(matchId: Long, type: TimerType, seconds: Long) {
        stringRedisTemplate.opsForValue().set(makeTimerKey(matchId, type), seconds.toString())
    }

    fun decreaseTimer(matchId: Long, type: TimerType): Long =
        stringRedisTemplate.opsForValue().decrement(makeTimerKey(matchId, type)) ?: 0L

    fun abortTimer(matchId: Long, type: TimerType) {
        stringRedisTemplate.delete(makeTimerKey(matchId, type))
    }

    private fun makeRefereesKey(matchId: Long) = "banchus:multiplayer:referees:$matchId"

    private fun makeSlotsKey(matchId: Long) = "banchus:multiplayer:$matchId"

    private fun makeTimerKey(matchId: Long, type: TimerType) =
        when (type) {
            TimerType.REGULAR -> "banchus:multiplayer:timer:$matchId"
            TimerType.MATCH_START -> "banchus:multiplayer:start_timer:$matchId"
        }
}
