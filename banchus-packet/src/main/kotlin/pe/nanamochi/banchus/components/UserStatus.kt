package pe.nanamochi.banchus.components

data class UserStatus(
    var action: UByte = 0u,
    var infoText: String = "",
    var mods: List<Mods> = emptyList(),
    var gamemode: Mode = Mode.OSU,
    var beatmapMd5: String = "",
    var beatmapId: Int = -1,
)
