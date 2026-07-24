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
import pe.nanamochi.banchus.packets.client.MatchCompletePacket

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_COMPLETE)
class MatchCompleteHandler(
    private val multiplayerService: MultiplayerService,
    private val broadcaster: MultiplayerBroadcaster,
) : AbstractPacketHandler<MatchCompletePacket>(PacketType.OSU_MATCH_COMPLETE) {
    override fun handle(
        packet: MatchCompletePacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.playerCompleted(session).onSuccess { result ->
            if (result != null) {
                broadcaster.matchComplete(result.match.matchId)
                broadcaster.matchUpdate(result.match, result.slots)
            }
        }
    }
}
