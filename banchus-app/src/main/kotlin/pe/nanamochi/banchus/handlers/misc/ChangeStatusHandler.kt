package pe.nanamochi.banchus.handlers.misc

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.ChangeStatusPacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.PresenceService

@Component
@HandleClientPacket(type = PacketType.OSU_CHANGE_STATUS, checkForRestriction = true)
class ChangeStatusHandler(private val presenceService: PresenceService) :
    AbstractPacketHandler<ChangeStatusPacket>(PacketType.OSU_CHANGE_STATUS) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: ChangeStatusPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        presenceService.handleChangeStatus(packet, session).onFailure { error ->
            log.warn(
                "Failed to update and broadcast status for user {}: {}",
                session.username,
                error,
            )
        }
    }
}
