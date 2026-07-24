package pe.nanamochi.banchus.multiplayer.handlers

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.multiplayer.broadcast.MultiplayerBroadcaster
import pe.nanamochi.banchus.multiplayer.service.MultiplayerService
import pe.nanamochi.banchus.packets.client.MatchStartPacket

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_START)
class MatchStartHandler(
    private val multiplayerService: MultiplayerService,
    private val broadcaster: MultiplayerBroadcaster,
) : AbstractPacketHandler<MatchStartPacket>(PacketType.OSU_MATCH_START) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MatchStartPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.fetchSessionMatchId(session.sessionId)?.let { matchId ->
            multiplayerService
                .startGame(matchId, session.userId)
                .onSuccess { (match, slots) -> broadcaster.matchStart(matchId, match, slots) }
                .onFailure { log.warn("Failed to start match #$matchId: $it") }
        }
    }
}
