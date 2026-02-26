package pe.nanamochi.banchus.components

enum class SlotStatus(val value: Int) {
    OPEN(1),
    LOCKED(1 shl 1),
    NOT_READY(1 shl 2),
    READY(1 shl 3),
    NO_BEATMAP(1 shl 4),
    PLAYING(1 shl 5),
    COMPLETE(1 shl 6),
    QUIT(1 shl 7);

    companion object {
        val HAS_PLAYER: Int
            get() =
                NOT_READY.value or
                    READY.value or
                    NO_BEATMAP.value or
                    PLAYING.value or
                    COMPLETE.value

        val CAN_START: Int
            get() = NOT_READY.value or READY.value

        val WAITING_FOR_END: Int
            get() = PLAYING.value or COMPLETE.value

        fun fromValue(value: Int): SlotStatus = entries.find { it.value == value } ?: OPEN

        fun fromBitmask(bitmask: Int): List<SlotStatus> =
            entries.filter { (bitmask and it.value) != 0 }

        fun toBitmask(statuses: List<SlotStatus>): Int =
            statuses.fold(0) { acc, status -> acc or status.value }
    }
}
