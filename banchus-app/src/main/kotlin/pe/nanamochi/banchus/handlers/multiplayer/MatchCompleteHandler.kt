package pe.nanamochi.banchus.handlers.multiplayer

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchCompletePacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_COMPLETE)
class MatchCompleteHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchCompletePacket>(PacketType.OSU_MATCH_COMPLETE) {
    override fun handle(
        packet: MatchCompletePacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.playerCompleted(session)
    }
}
