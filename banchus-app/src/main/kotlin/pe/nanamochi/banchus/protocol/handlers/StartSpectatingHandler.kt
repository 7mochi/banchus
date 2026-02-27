package pe.nanamochi.banchus.protocol.handlers

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.StartSpectatingPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.SpectatorService

@Component
@HandleClientPacket(type = PacketType.OSU_START_SPECTATING, checkForRestriction = true)
class StartSpectatingHandler(private val spectatorService: SpectatorService) :
    AbstractPacketHandler<StartSpectatingPacket>(PacketType.OSU_START_SPECTATING) {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: StartSpectatingPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        spectatorService.startSpectating(session, packet).onFailure { error ->
            log.warn(
                "Failed to start spectating {} for user {}: {}",
                packet.userId,
                session.user?.username,
                error,
            )
        }
    }
}
