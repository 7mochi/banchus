package pe.nanamochi.banchus.handlers.multiplayer

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.domain.enums.Mods
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchChangeModsPacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_CHANGE_MODS)
class MatchChangeModsHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchChangeModsPacket>(PacketType.OSU_MATCH_CHANGE_MODS) {
    override fun handle(
        packet: MatchChangeModsPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.fetchSessionMatchId(session.sessionId)?.let { matchId ->
            multiplayerService.changeMods(
                matchId,
                Mods.fromBitmask(packet.mods),
                session.identity(),
            )
        }
    }
}
