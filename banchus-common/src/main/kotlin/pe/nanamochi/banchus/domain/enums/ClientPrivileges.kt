package pe.nanamochi.banchus.domain.enums

enum class ClientPrivileges(val value: Int) {
    PLAYER(1),
    MODERATOR(1 shl 1),
    SUPPORTER(1 shl 2),
    OWNER(1 shl 3),
    DEVELOPER(1 shl 4),
    TOURNAMENT(1 shl 5),
    // Note: not used in communications with osu! client
}
