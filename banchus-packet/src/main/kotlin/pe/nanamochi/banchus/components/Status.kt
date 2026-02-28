package pe.nanamochi.banchus.components

enum class Status(val value: Int) {
    IDLE(0),
    AFK(1),
    PLAYING(2),
    EDITING(3),
    MODDING(4),
    MULTIPLAYER(5),
    WATCHING(6),
    UNKNOWN(7),
    TESTING(8),
    SUBMITTING(9),
    PAUSED(10),
    LOBBY(11),
    MULTIPLAYING(12),
    OSU_DIRECT(13);

    companion object {
        private val map = entries.associateBy(Status::value)

        fun fromValue(value: Int): Status = map[value] ?: UNKNOWN
    }
}
