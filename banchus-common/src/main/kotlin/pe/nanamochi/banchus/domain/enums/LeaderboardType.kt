package pe.nanamochi.banchus.domain.enums

enum class LeaderboardType(val value: Int) {
    LOCAL(0),
    GLOBAL(1),
    MODS(2),
    FRIENDS(3),
    COUNTRY(4);

    companion object {
        fun fromValue(value: Int): LeaderboardType {
            return entries.find { it.value == value } ?: GLOBAL
        }
    }
}
