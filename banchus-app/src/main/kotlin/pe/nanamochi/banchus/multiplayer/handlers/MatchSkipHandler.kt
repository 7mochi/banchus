package pe.nanamochi.banchus.multiplayer.handlers

import com.github.michaelbull.result.onSuccess
import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.multiplayer.broadcast.MultiplayerBroadcaster
import pe.nanamochi.banchus.multiplayer.service.MultiplayerService
import pe.nanamochi.banchus.packets.client.MatchSkipPacket

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_SKIP)
class MatchSkipHandler(
    private val multiplayerService: MultiplayerService,
    private val broadcaster: MultiplayerBroadcaster,
) : AbstractPacketHandler<MatchSkipPacket>(PacketType.OSU_MATCH_SKIP) {
    override fun handle(
        packet: MatchSkipPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.skipRequested(session).onSuccess { result ->
            broadcaster.playerSkipped(result.matchId, result.slotId, result.allSkipped)
        }
    }
}
