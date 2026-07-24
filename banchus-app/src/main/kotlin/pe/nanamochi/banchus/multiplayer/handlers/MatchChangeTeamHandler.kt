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
import pe.nanamochi.banchus.packets.client.MatchChangeTeamPacket

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_CHANGE_TEAM)
class MatchChangeTeamHandler(
    private val multiplayerService: MultiplayerService,
    private val broadcaster: MultiplayerBroadcaster,
) : AbstractPacketHandler<MatchChangeTeamPacket>(PacketType.OSU_MATCH_CHANGE_TEAM) {
    override fun handle(
        packet: MatchChangeTeamPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        val matchId = multiplayerService.fetchSessionMatchId(session.sessionId) ?: return
        multiplayerService.switchTeams(matchId, session.sessionId).onSuccess { (match, slots) ->
            broadcaster.matchUpdate(match, slots)
        }
    }
}
