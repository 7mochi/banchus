package pe.nanamochi.banchus.domain.enums

enum class ScoringType(val value: Int) {
    SCORE(0),
    ACCURACY(1),
    COMBO(2),
    SCORE_V2(3);

    companion object {
        fun fromValue(value: Int): ScoringType = entries.find { it.value == value } ?: SCORE
    }
}
