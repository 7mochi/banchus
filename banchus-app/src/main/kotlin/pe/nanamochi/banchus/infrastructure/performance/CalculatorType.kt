package pe.nanamochi.banchus.infrastructure.performance

enum class CalculatorType(val alias: String) {
    OSU_NATIVE("osu-native"),
    ROSU("rosu-pp");

    companion object {
        fun fromAlias(alias: String?): CalculatorType =
            entries.find { it.alias.equals(alias, ignoreCase = true) } ?: ROSU
    }
}
