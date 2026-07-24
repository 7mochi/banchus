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
import pe.nanamochi.banchus.packets.client.MatchFailedPacket

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_FAILED)
class MatchFailedHandler(
    private val multiplayerService: MultiplayerService,
    private val broadcaster: MultiplayerBroadcaster,
) : AbstractPacketHandler<MatchFailedPacket>(PacketType.OSU_MATCH_FAILED) {
    override fun handle(
        packet: MatchFailedPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.playerFailed(session).onSuccess { result ->
            broadcaster.playerFailed(result.matchId, result.slotId)
        }
    }
}
