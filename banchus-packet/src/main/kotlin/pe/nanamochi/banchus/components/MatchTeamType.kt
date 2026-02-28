package pe.nanamochi.banchus.components

enum class MatchTeamType(val value: Int) {
    HEAD_TO_HEAD(0),
    TAG_COOP(1),
    TEAM_VS(2),
    TAG_TEAM_VS(3);

    companion object {
        fun fromValue(value: Int): MatchTeamType =
            entries.find { it.value == value } ?: HEAD_TO_HEAD
    }
}
