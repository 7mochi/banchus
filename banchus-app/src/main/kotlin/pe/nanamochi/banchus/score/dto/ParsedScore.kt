package pe.nanamochi.banchus.score.dto

import pe.nanamochi.banchus.score.entity.Score

data class ParsedScore(
    val score: Score,
    val replayBytes: ByteArray,
    val beatmapMd5: String,
    val username: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParsedScore) return false

        if (score != other.score) return false
        if (!replayBytes.contentEquals(other.replayBytes)) return false
        if (beatmapMd5 != other.beatmapMd5) return false
        if (username != other.username) return false

        return true
    }

    override fun hashCode(): Int {
        var result = score.hashCode()
        result = 31 * result + replayBytes.contentHashCode()
        result = 31 * result + beatmapMd5.hashCode()
        result = 31 * result + username.hashCode()
        return result
    }
}
