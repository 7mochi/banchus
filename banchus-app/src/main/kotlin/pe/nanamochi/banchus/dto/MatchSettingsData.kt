package pe.nanamochi.banchus.dto

import pe.nanamochi.banchus.domain.enums.MatchTeamType
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.domain.enums.ScoringType

data class MatchSettingsData(
    val name: String,
    val password: String?,
    val beatmapName: String,
    val beatmapId: Int,
    val beatmapMd5: String,
    val mode: Mode,
    val mods: UInt,
    val scoringType: ScoringType,
    val teamType: MatchTeamType,
    val freemodsEnabled: Boolean,
    val randomSeed: Int,
)
