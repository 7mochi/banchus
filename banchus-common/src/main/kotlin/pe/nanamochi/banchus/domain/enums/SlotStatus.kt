package pe.nanamochi.banchus.domain.enums

enum class SlotStatus(val value: Int) {
    NONE(0),
    OPEN(1),
    LOCKED(1 shl 1),
    NOT_READY(1 shl 2),
    READY(1 shl 3),
    NO_BEATMAP(1 shl 4),
    PLAYING(1 shl 5),
    COMPLETE(1 shl 6),
    QUIT(1 shl 7);

    companion object {
        fun fromValue(value: Int): SlotStatus = entries.find { it.value == value } ?: OPEN

        fun fromBitmask(bitmask: Int): List<SlotStatus> =
            entries.filter { (bitmask and it.value) != 0 }

        fun toBitmask(statuses: List<SlotStatus>): Int =
            statuses.fold(0) { acc, status -> acc or status.value }
    }
}
