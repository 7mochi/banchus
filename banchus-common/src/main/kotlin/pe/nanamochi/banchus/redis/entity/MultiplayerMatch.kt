package pe.nanamochi.banchus.redis.entity

import kotlin.toUShort
import pe.nanamochi.banchus.domain.enums.SlotStatus

data class MultiplayerMatch(
    var matchId: Long = 0,
    var name: String = "",
    var password: String? = null,
    var inProgress: Boolean = false,
    var powerplay: Boolean = false,
    var mods: UInt = 0u,
    var beatmapName: String = "",
    var beatmapMd5: String = "",
    var beatmapId: Int = 0,
    var hostUserId: Int = 0,
    var mode: UByte = 0u,
    var winCondition: UByte = 0u,
    var teamType: UByte = 0u,
    var freemodEnabled: Boolean = false,
    var randomSeed: Int = 0,
    var lastGameId: Int? = null,
) {
    fun inGameMatchId(): UShort {
        return (matchId and 0xFFFFL).toUShort()
    }
}

data class MultiplayerMatchSlot(
    var status: SlotStatus = SlotStatus.OPEN,
    var team: UByte = 0u,
    var mods: UInt = 0u,
    var user: SessionIdentity? = null,
    var loaded: Boolean = false,
    var skipped: Boolean = false,
    var failed: Boolean = false,
    var completed: Boolean = false,
) {
    fun prepare(identity: SessionIdentity) {
        this.status = SlotStatus.NOT_READY
        this.team = 0u
        this.mods = 0u
        this.user = identity
        this.loaded = false
        this.skipped = false
        this.failed = false
        this.completed = false
    }

    fun clear() {
        this.status = SlotStatus.OPEN
        this.team = 0u
        this.mods = 0u
        this.user = null
        this.loaded = false
        this.skipped = false
        this.failed = false
        this.completed = false
    }
}
