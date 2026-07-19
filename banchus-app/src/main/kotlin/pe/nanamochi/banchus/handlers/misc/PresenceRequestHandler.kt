package pe.nanamochi.banchus.handlers.misc

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.PresenceRequestPacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.PresenceService

@Component
@HandleClientPacket(type = PacketType.OSU_PRESENCE_REQUEST, checkForRestriction = true)
class PresenceRequestHandler(private val presenceService: PresenceService) :
    AbstractPacketHandler<PresenceRequestPacket>(PacketType.OSU_PRESENCE_REQUEST) {
    override fun handle(
        packet: PresenceRequestPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        presenceService.handlePresenceRequest(packet, responseStream)
    }
}
