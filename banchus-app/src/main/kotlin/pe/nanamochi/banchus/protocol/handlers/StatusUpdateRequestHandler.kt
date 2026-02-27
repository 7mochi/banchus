package pe.nanamochi.banchus.protocol.handlers

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.StatusUpdateRequestPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.PresenceService

@Component
@HandleClientPacket(type = PacketType.OSU_STATUS_UPDATE_REQUEST, checkForRestriction = true)
class StatusUpdateRequestHandler(private val presenceService: PresenceService) :
    AbstractPacketHandler<StatusUpdateRequestPacket>(PacketType.OSU_STATUS_UPDATE_REQUEST) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: StatusUpdateRequestPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        presenceService.broadcastSelfStats(session).onFailure { error ->
            log.warn(
                "Failed to send self-stats update to user {}: {}",
                session.user?.username,
                error,
            )
        }
    }
}
