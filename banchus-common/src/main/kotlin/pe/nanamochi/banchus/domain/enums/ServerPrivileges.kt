package pe.nanamochi.banchus.domain.enums

enum class ServerPrivileges(val value: Int) {
    NONE(0),
    UNRESTRICTED(1),
    SUBMITTED_HARDWARE_IDENTITY(1 shl 1),
    DONATOR(1 shl 2),
    ADMIN_ACCESS_PANEL(1 shl 3),
    SUPPORTER(1 shl 4),
    BEATMAP_NOMINATOR(1 shl 7),
    CHAT_MODERATOR(1 shl 9),
    MULTIPLAYER_STAFF(1 shl 11),
    ACCOUNT_MANAGEMENT(1 shl 13),
    TOURNAMENT_STAFF(1 shl 21),
    SUPER_ADMIN(1 shl 30);

    companion object {
        fun fromBitmask(bitmask: Int): List<ServerPrivileges> {
            if (bitmask == 0) return emptyList()

            return entries.filter { (bitmask and it.value) != 0 }
        }

        fun toBitmask(privileges: List<ServerPrivileges>?): Int =
            privileges?.fold(0) { acc, privilege -> acc or privilege.value } ?: 0
    }
}
