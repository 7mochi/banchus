package pe.nanamochi.banchus.handlers.multiplayer

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchLoadCompletePacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_LOAD_COMPLETE)
class MatchLoadCompleteHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchLoadCompletePacket>(PacketType.OSU_MATCH_LOAD_COMPLETE) {
    override fun handle(
        packet: MatchLoadCompletePacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.playerLoaded(session)
    }
}
