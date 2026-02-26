package pe.nanamochi.banchus.domain.enums

enum class Mode(val value: UByte, val formatted: String, val alias: String) {
    OSU(0u, "osu!", "osu"),
    TAIKO(1u, "Taiko", "taiko"),
    CATCH(2u, "CatchTheBeat", "fruits"),
    MANIA(3u, "osu!mania", "mania");

    companion object {
        private val ALIAS_MAP =
            mapOf(
                "std" to OSU,
                "osu" to OSU,
                "taiko" to TAIKO,
                "fruits" to CATCH,
                "ctb" to CATCH,
                "catch" to CATCH,
                "mania" to MANIA,
            )

        fun fromAlias(input: String?): Mode = input?.lowercase()?.let { ALIAS_MAP[it] } ?: OSU

        fun fromValue(value: UByte): Mode = entries.find { it.value == value } ?: OSU

        @JvmStatic
        fun fromValue(value: Int): Mode = entries.find { it.value.toInt() == value } ?: OSU
    }
}
