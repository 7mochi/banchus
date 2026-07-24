package pe.nanamochi.banchus.multiplayer.handlers

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.toResultOr
import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.StreamName
import pe.nanamochi.banchus.core.error.MatchNotFound
import pe.nanamochi.banchus.core.service.StreamService
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.multiplayer.enums.SlotStatus
import pe.nanamochi.banchus.multiplayer.service.MultiplayerService
import pe.nanamochi.banchus.packets.client.MatchScoreUpdatePacket
import pe.nanamochi.banchus.protocol.PacketWriter

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_SCORE_UPDATE)
class MatchScoreUpdateHandler(
    private val multiplayerService: MultiplayerService,
    private val packetWriter: PacketWriter,
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
            streamService.broadcastData(
                StreamName.Multiplaying(matchId),
                packetWriter.serialize(
                    pe.nanamochi.banchus.packets.server.MatchScoreUpdatePacket(frame = packet.frame)
                ),
            )
        }
    }
}
