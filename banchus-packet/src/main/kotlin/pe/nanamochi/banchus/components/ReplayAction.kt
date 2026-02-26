package pe.nanamochi.banchus.components

enum class ReplayAction(val value: Int) {
    STANDARD(0),
    NEW_SONG(1),
    SKIP(2),
    COMPLETION(3),
    FAIL(4),
    PAUSE(5),
    UNPAUSE(6),
    SONG_SELECT(7),
    WATCHING_OTHER(8);

    companion object {
        private val map = entries.associateBy(ReplayAction::value)

        fun fromValue(value: Int): ReplayAction = map[value] ?: STANDARD
    }
}
