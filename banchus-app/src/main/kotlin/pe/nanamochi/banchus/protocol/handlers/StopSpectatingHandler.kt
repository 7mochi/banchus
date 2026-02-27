package pe.nanamochi.banchus.protocol.handlers

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.StopSpectatingPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.SpectatorService

@Component
@HandleClientPacket(type = PacketType.OSU_STOP_SPECTATING, checkForRestriction = true)
class StopSpectatingHandler(private val spectatorService: SpectatorService) :
    AbstractPacketHandler<StopSpectatingPacket>(PacketType.OSU_STOP_SPECTATING) {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: StopSpectatingPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        spectatorService.stopSpectating(session).onFailure { error ->
            log.warn("Failed to stop spectating for user {}: {}", session.user?.username, error)
        }
    }
}
