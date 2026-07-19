package pe.nanamochi.banchus.handlers.multiplayer

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.toResultOr
import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.domain.enums.SlotStatus
import pe.nanamochi.banchus.domain.error.MatchNotFound
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchScoreUpdatePacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.redis.stream.StreamName
import pe.nanamochi.banchus.service.MultiplayerService
import pe.nanamochi.banchus.service.StreamService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_SCORE_UPDATE)
class MatchScoreUpdateHandler(
    private val multiplayerService: MultiplayerService,
    private val streamService: StreamService,
) : AbstractPacketHandler<MatchScoreUpdatePacket>(PacketType.OSU_MATCH_SCORE_UPDATE) {
    override fun handle(
        packet: MatchScoreUpdatePacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        binding {
            val matchId =
                multiplayerService
                    .fetchSessionMatchId(session.sessionId)
                    .toResultOr { MatchNotFound }
                    .bind()
            val (slotId, slot) =
                multiplayerService.fetchSessionSlot(matchId, session.sessionId).bind()
            if (slot.status != SlotStatus.PLAYING) return@binding

            packet.frame.id = slotId
            streamService.broadcastMessage(
                StreamName.Multiplaying(matchId),
                pe.nanamochi.banchus.packets.server.MatchScoreUpdatePacket(frame = packet.frame),
            )
        }
    }
}
