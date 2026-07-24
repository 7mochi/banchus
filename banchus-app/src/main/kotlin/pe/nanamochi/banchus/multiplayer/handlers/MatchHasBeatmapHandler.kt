package pe.nanamochi.banchus.multiplayer.handlers

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.multiplayer.enums.SlotStatus
import pe.nanamochi.banchus.multiplayer.service.MultiplayerService
import pe.nanamochi.banchus.packets.client.MatchHasBeatmapPacket

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_HAS_BEATMAP)
class MatchHasBeatmapHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchHasBeatmapPacket>(PacketType.OSU_MATCH_HAS_BEATMAP) {
    override fun handle(
        packet: MatchHasBeatmapPacket,
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
