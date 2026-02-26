package pe.nanamochi.banchus.dto

data class ScoreUpdateData(
    val time: Int,
    val total300: Int,
    val total100: Int,
    val total50: Int,
    val totalGeki: Int,
    val totalKatu: Int,
    val totalMiss: Int,
    val totalScore: Int,
    val maxCombo: Int,
    val currentCombo: Int,
    val perfect: Boolean,
    val hp: Int,
    val tagByte: Int,
    val usingScoreV2: Boolean,
    val comboPortion: Double,
    val bonusPortion: Double,
)
