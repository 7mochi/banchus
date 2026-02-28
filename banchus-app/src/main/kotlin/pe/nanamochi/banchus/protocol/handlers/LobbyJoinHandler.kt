package pe.nanamochi.banchus.protocol.handlers

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.LobbyJoinPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.MultiplayerService
import pe.nanamochi.banchus.service.SessionService

@Component
@HandleClientPacket(type = PacketType.OSU_LOBBY_JOIN)
class LobbyJoinHandler(
    private val multiplayerService: MultiplayerService,
    private val sessionService: SessionService,
) : AbstractPacketHandler<LobbyJoinPacket>(PacketType.OSU_LOBBY_JOIN) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: LobbyJoinPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        binding {
                session.receiveMatchUpdates = true
                sessionService.update(session).bind()
                multiplayerService.sendCurrentMatches(session).bind()
            }
            .onFailure { error ->
                log.error("Lobby join failed for {}: {}", session.user?.username, error)
            }
    }
}
