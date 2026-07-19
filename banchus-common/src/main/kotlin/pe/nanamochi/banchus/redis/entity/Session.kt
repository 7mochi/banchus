package pe.nanamochi.banchus.redis.entity

import java.time.Duration
import java.time.Instant
import java.util.UUID
import pe.nanamochi.banchus.domain.enums.ServerPrivileges

data class Session(
    var sessionId: UUID = UUID.randomUUID(),
    var userId: Int,
    var username: String,
    var privileges: Int,
    var createIpAddress: String,
    var privateDms: Boolean,
    var silenceEnd: Instant? = null,
    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
) {
    val isSilenced: Boolean
        get() = silenceEnd?.isAfter(Instant.now()) ?: false

    val isRestricted: Boolean
        get() = !ServerPrivileges.fromBitmask(privileges).contains(ServerPrivileges.UNRESTRICTED)

    val isDonor: Boolean
        get() = ServerPrivileges.fromBitmask(privileges).contains(ServerPrivileges.DONATOR)

    val isStaff: Boolean
        get() = ServerPrivileges.fromBitmask(privileges).contains(ServerPrivileges.CHAT_MODERATOR)

    val isDeveloper: Boolean
        get() = ServerPrivileges.fromBitmask(privileges).contains(ServerPrivileges.SUPER_ADMIN)

    val isTournamentStaff: Boolean
        get() = ServerPrivileges.fromBitmask(privileges).contains(ServerPrivileges.TOURNAMENT_STAFF)

    val silenceLeft: Int
        get() {
            val end = silenceEnd ?: return 0
            val now = Instant.now()

            return if (end.isAfter(now)) {
                Duration.between(now, end).seconds.toInt()
            } else {
                0
            }
        }

    fun identity() = SessionIdentity(sessionId = sessionId, userId = userId)
}

data class SessionIdentity(var sessionId: UUID, var userId: Int)
