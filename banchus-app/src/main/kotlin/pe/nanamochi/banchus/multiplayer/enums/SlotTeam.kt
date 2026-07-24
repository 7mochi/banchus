package pe.nanamochi.banchus.multiplayer.enums

enum class SlotTeam(val value: UByte) {
    NEUTRAL(0u),
    BLUE(1u),
    RED(2u);

    companion object {
        fun fromValue(value: UByte): SlotTeam = entries.find { it.value == value } ?: NEUTRAL
    }
}
