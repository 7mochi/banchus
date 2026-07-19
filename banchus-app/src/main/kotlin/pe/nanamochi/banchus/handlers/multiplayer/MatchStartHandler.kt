package pe.nanamochi.banchus.handlers.multiplayer

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchStartPacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_START)
class MatchStartHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchStartPacket>(PacketType.OSU_MATCH_START) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MatchStartPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.fetchSessionMatchId(session.sessionId)?.let { matchId ->
            multiplayerService.startGame(matchId, session.userId).onFailure {
                log.warn("Failed to start match #$matchId: $it")
            }
        }
    }
}
