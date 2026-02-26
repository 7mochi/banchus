package pe.nanamochi.banchus.components

enum class QuitState(val value: Int) {
    GONE(0),
    OSU_REMAINING(1),
    IRC_REMAINING(2);

    companion object {
        private val map = entries.associateBy(QuitState::value)

        fun fromValue(value: Int): QuitState = map[value] ?: GONE
    }
}
