package pe.nanamochi.banchus.multiplayer.handlers

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.multiplayer.enums.SlotStatus
import pe.nanamochi.banchus.multiplayer.service.MultiplayerService
import pe.nanamochi.banchus.packets.client.MatchNoBeatmapPacket

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_NO_BEATMAP)
class MatchNoBeatmapHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchNoBeatmapPacket>(PacketType.OSU_MATCH_NO_BEATMAP) {
    override fun handle(
        packet: MatchNoBeatmapPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.fetchSessionMatchId(session.sessionId)?.let { matchId ->
            multiplayerService.setSessionSlotStatus(
                matchId,
                session.sessionId,
                SlotStatus.NO_BEATMAP,
            )
        }
    }
}
