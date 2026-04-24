package pe.nanamochi.banchus.handlers.spectator

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.StartSpectatingPacket
import pe.nanamochi.banchus.redis.entity.Session
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
        spectatorService.handleStartSpectating(packet, session, responseStream).onFailure { error ->
            log.warn("Failed to start spectating for session {}: {}", session.sessionId, error)
        }
    }
}
