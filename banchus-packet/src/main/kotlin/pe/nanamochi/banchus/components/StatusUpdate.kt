package pe.nanamochi.banchus.components

data class StatusUpdate(
    var status: Status = Status.IDLE,
    var text: String = "",
    var beatmapMd5: String = "",
    var mods: List<Mods> = emptyList(),
    var mode: Mode = Mode.OSU,
    var beatmapId: Int = 0,
)
