package pe.nanamochi.banchus.spectator.handlers

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.InvalidSpectateTarget
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.StartSpectatingPacket
import pe.nanamochi.banchus.spectator.broadcast.SpectatorBroadcaster
import pe.nanamochi.banchus.spectator.service.SpectatorService

@Component
@HandleClientPacket(type = PacketType.OSU_START_SPECTATING, checkForRestriction = true)
class StartSpectatingHandler(
    private val spectatorService: SpectatorService,
    private val broadcaster: SpectatorBroadcaster,
) : AbstractPacketHandler<StartSpectatingPacket>(PacketType.OSU_START_SPECTATING) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: StartSpectatingPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        val result =
            binding<Unit, DomainMessage> {
                if (packet.userId == 1 || packet.userId == session.userId) {
                    Err(InvalidSpectateTarget).bind()
                }

                val spectators = spectatorService.join(session, packet.userId).bind()
                responseStream.write(broadcaster.startSpectatingResponse(spectators, session))
            }

        result.onFailure { error ->
            log.warn("Failed to start spectating for session {}: {}", session.sessionId, error)
        }
    }
}
