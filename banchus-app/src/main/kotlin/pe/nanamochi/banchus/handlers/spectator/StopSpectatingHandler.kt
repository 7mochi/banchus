package pe.nanamochi.banchus.handlers.spectator

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.StopSpectatingPacket
import pe.nanamochi.banchus.redis.entity.Session
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
        spectatorService.leave(session, null).onFailure { error ->
            log.warn("Failed to stop spectating for session {}: {}", session.sessionId, error)
        }
    }
}
