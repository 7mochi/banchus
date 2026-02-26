package pe.nanamochi.banchus.domain.enums

enum class BeatmapRankedStatus(val value: Int) {
    GRAVEYARD(-2),
    WIP(-1),
    PENDING(0),
    RANKED(1),
    APPROVED(2),
    QUALIFIED(3),
    LOVED(4);

    companion object {
        @JvmStatic
        fun fromValue(value: Int): BeatmapRankedStatus =
            entries.find { it.value == value } ?: GRAVEYARD
    }
}
