package pe.nanamochi.banchus.components

enum class MatchType(val value: Int) {
    STANDARD(0),
    POWERPLAY(1);

    companion object {
        fun fromValue(value: Int): MatchType = entries.find { it.value == value } ?: STANDARD
    }
}
