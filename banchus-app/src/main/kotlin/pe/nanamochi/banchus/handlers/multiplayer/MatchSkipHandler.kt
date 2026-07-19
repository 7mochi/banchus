package pe.nanamochi.banchus.handlers.multiplayer

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchSkipPacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_SKIP)
class MatchSkipHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchSkipPacket>(PacketType.OSU_MATCH_SKIP) {
    override fun handle(
        packet: MatchSkipPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.skipRequested(session)
    }
}
