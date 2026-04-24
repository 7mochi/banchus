package pe.nanamochi.banchus.handlers.multiplayer

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.domain.enums.SlotStatus
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchNotReadyPacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_NOT_READY)
class MatchNotReadyHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchNotReadyPacket>(PacketType.OSU_MATCH_NOT_READY) {
    override fun handle(
        packet: MatchNotReadyPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.fetchSessionMatchId(session.sessionId)?.let { matchId ->
            multiplayerService.setSessionSlotStatus(
                matchId,
                session.sessionId,
                SlotStatus.NOT_READY,
            )
        }
    }
}
