package pe.nanamochi.banchus.util

import pe.nanamochi.banchus.components.MatchSlot
import pe.nanamochi.banchus.components.MatchTeamType
import pe.nanamochi.banchus.components.Mode
import pe.nanamochi.banchus.components.ScoringType
import pe.nanamochi.banchus.components.SlotTeam
import pe.nanamochi.banchus.redis.entity.MultiplayerMatch
import pe.nanamochi.banchus.redis.entity.MultiplayerMatchSlot

fun MultiplayerMatch.asBancho(slots: List<MultiplayerMatchSlot>) =
    pe.nanamochi.banchus.components.Match(
        id = inGameMatchId().toInt(),
        inProgress = inProgress,
        mods = mods,
        name = name,
        password = password,
        beatmapName = beatmapName,
        beatmapMd5 = beatmapMd5,
        beatmapId = beatmapId,
        slots = slots.asBancho(),
        hostId = hostUserId,
        mode = Mode.fromValue(mode),
        scoringType = ScoringType.fromValue(winCondition.toInt()),
        teamType = MatchTeamType.fromValue(teamType.toInt()),
        freemodsEnabled = freemodEnabled,
        randomSeed = randomSeed.toUInt(),
    )

fun List<MultiplayerMatchSlot>.asBancho() =
    this.map { slot ->
        MatchSlot(
            userId = slot.user?.userId ?: 0,
            status = slot.status.value.toByte(),
            team = SlotTeam.fromValue(slot.team.value.toInt()),
            mods = slot.mods,
        )
    }
