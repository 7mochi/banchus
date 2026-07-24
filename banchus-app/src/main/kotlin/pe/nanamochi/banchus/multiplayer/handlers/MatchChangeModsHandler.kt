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
import pe.nanamochi.banchus.packets.client.MatchChangeModsPacket
import pe.nanamochi.banchus.core.enums.Mods

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_CHANGE_MODS)
class MatchChangeModsHandler(
    private val multiplayerService: MultiplayerService,
    private val broadcaster: MultiplayerBroadcaster,
) : AbstractPacketHandler<MatchChangeModsPacket>(PacketType.OSU_MATCH_CHANGE_MODS) {
    override fun handle(
        packet: MatchChangeModsPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        val matchId = multiplayerService.fetchSessionMatchId(session.sessionId) ?: return
        multiplayerService
            .changeMods(matchId, Mods.fromBitmask(packet.mods), session.identity())
            .onSuccess { (match, slots) -> broadcaster.matchUpdate(match, slots) }
    }
}
