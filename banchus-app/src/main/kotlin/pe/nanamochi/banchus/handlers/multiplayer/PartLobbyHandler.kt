package pe.nanamochi.banchus.handlers.multiplayer

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.PartLobbyPacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.redis.stream.StreamName
import pe.nanamochi.banchus.service.StreamService

@Component
@HandleClientPacket(type = PacketType.OSU_PART_LOBBY)
class PartLobbyHandler(private val streamService: StreamService) :
    AbstractPacketHandler<PartLobbyPacket>(PacketType.OSU_PART_LOBBY) {
    override fun handle(
        packet: PartLobbyPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        streamService.leave(session.sessionId, StreamName.Lobby)
    }
}
