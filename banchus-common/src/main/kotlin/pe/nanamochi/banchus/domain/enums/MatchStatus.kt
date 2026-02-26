package pe.nanamochi.banchus.domain.enums

enum class MatchStatus(val value: Int) {
    WAITING(0),
    PLAYING(1);

    companion object {
        fun fromValue(value: Int): MatchStatus = entries.find { it.value == value } ?: WAITING
    }
}
