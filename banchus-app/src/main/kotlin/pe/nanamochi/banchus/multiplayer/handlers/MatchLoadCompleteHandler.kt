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
import pe.nanamochi.banchus.packets.client.MatchLoadCompletePacket

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_LOAD_COMPLETE)
class MatchLoadCompleteHandler(
    private val multiplayerService: MultiplayerService,
    private val broadcaster: MultiplayerBroadcaster,
) : AbstractPacketHandler<MatchLoadCompletePacket>(PacketType.OSU_MATCH_LOAD_COMPLETE) {
    override fun handle(
        packet: MatchLoadCompletePacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.playerLoaded(session).onSuccess { result ->
            if (result.allLoaded) {
                broadcaster.allPlayersLoaded(result.matchId)
            }
        }
    }
}
