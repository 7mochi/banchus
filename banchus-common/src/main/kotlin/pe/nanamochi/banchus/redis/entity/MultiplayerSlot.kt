package pe.nanamochi.banchus.redis.entity

import java.util.UUID
import pe.nanamochi.banchus.domain.enums.SlotTeam

data class MultiplayerSlot(
    var slotId: Int = 0,
    var sessionId: UUID? = null,
    var userId: Int = 0,
    var status: Byte = 0,
    var team: SlotTeam = SlotTeam.NEUTRAL,
    var mods: UInt = 0u,
    var isLoaded: Boolean = false,
    var isSkipped: Boolean = false,
)
