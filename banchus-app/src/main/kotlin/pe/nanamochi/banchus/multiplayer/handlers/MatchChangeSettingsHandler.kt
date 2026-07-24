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
import pe.nanamochi.banchus.packets.client.MatchChangeSettingsPacket

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_CHANGE_SETTINGS)
class MatchChangeSettingsHandler(
    private val multiplayerService: MultiplayerService,
    private val broadcaster: MultiplayerBroadcaster,
) : AbstractPacketHandler<MatchChangeSettingsPacket>(PacketType.OSU_MATCH_CHANGE_SETTINGS) {
    override fun handle(
        packet: MatchChangeSettingsPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        val matchId = multiplayerService.fetchSessionMatchId(session.sessionId) ?: return
        multiplayerService.changeSettings(matchId, packet.match, session.userId).onSuccess {
            (match, slots) ->
            broadcaster.matchUpdate(match, slots)
        }
    }
}
