package pe.nanamochi.banchus.components

data class MatchSlot(
    var userId: Int = -1,
    var status: Byte = 0,
    var team: SlotTeam = SlotTeam.NEUTRAL,
    var mods: UInt = 0u,
)
