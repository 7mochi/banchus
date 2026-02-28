package pe.nanamochi.banchus.protocol.handlers

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.UserStatusPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.PresenceService

@Component
@HandleClientPacket(type = PacketType.OSU_USER_STATUS, checkForRestriction = true)
class UserStatusHandler(private val presenceService: PresenceService) :
    AbstractPacketHandler<UserStatusPacket>(PacketType.OSU_USER_STATUS) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: UserStatusPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        presenceService.updateFromStatusPacket(session, packet).onFailure { error ->
            log.warn(
                "Failed to update and broadcast status for user {}: {}",
                session.user?.username,
                error,
            )
        }
    }
}
