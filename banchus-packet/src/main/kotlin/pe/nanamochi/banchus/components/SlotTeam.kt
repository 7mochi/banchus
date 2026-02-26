package pe.nanamochi.banchus.components

enum class SlotTeam(val value: Int) {
    NEUTRAL(0),
    BLUE(1),
    RED(2);

    companion object {
        fun fromValue(value: Int): SlotTeam = entries.find { it.value == value } ?: NEUTRAL
    }
}
