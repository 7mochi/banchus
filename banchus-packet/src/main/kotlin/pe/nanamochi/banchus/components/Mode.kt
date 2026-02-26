package pe.nanamochi.banchus.components

enum class Mode(val value: UByte) {
    OSU(0u),
    TAIKO(1u),
    CATCH(2u),
    MANIA(3u);

    companion object {
        fun fromValue(value: UByte): Mode = entries.find { it.value == value } ?: OSU
    }
}
