package pe.nanamochi.banchus.domain.enums

enum class BeatmapDirectDisplayMode(val value: Int, val apiStatus: Int) {
    RANKED(0, 1),
    RANKED_STRICT(1, 1),
    PENDING(2, 0),
    QUALIFIED(3, 3),
    ALL(4, -1),
    GRAVEYARD(5, -2),
    APPROVED(6, 2),
    RANKED_PLAYED(7, 1),
    LOVED(8, 4);

    companion object {
        fun fromValue(value: Int): BeatmapDirectDisplayMode {
            return entries.find { it.value == value } ?: ALL
        }
    }
}
