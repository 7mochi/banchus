package pe.nanamochi.banchus.redis.entity

import java.time.Instant
import pe.nanamochi.banchus.domain.enums.*

data class MultiplayerMatch(
    var matchId: Int = 0,
    var matchName: String = "",
    var matchPassword: String? = null,
    var beatmapName: String = "",
    var beatmapId: Int = 0,
    var beatmapMd5: String = "",
    var hostUserId: Int = 0,
    var mode: Mode = Mode.OSU,
    var mods: UInt = 0u,
    var scoringType: ScoringType = ScoringType.SCORE,
    var teamType: MatchTeamType = MatchTeamType.HEAD_TO_HEAD,
    var freemodsEnabled: Boolean = false,
    var randomSeed: Int = 0,
    var status: MatchStatus = MatchStatus.WAITING,
    var slots: MutableList<MultiplayerSlot> = mutableListOf(),
    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
) {
    fun allLoaded(): Boolean =
        slots.filter { it.status.toInt() and SlotStatus.PLAYING.value != 0 }.all { it.isLoaded }

    fun allSkipped(): Boolean =
        slots.filter { it.status.toInt() and SlotStatus.PLAYING.value != 0 }.all { it.isSkipped }

    fun allCompleted(): Boolean =
        slots
            .filter { it.status.toInt() and SlotStatus.PLAYING.value != 0 }
            .all { it.status.toInt() and SlotStatus.COMPLETE.value != 0 }
}
