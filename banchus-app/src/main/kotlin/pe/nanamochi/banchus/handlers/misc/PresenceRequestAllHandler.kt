package pe.nanamochi.banchus.handlers.misc

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.PresenceRequestAllPacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.PresenceService

@Component
@HandleClientPacket(type = PacketType.OSU_PRESENCE_REQUEST_ALL, checkForRestriction = true)
class PresenceRequestAllHandler(private val presenceService: PresenceService) :
    AbstractPacketHandler<PresenceRequestAllPacket>(PacketType.OSU_PRESENCE_REQUEST_ALL) {
    override fun handle(
        packet: PresenceRequestAllPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        presenceService.handlePresenceRequestAll(responseStream)
    }
}
