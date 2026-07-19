package pe.nanamochi.banchus.handlers.misc

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.UserStatsRequestPacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.PresenceService

@Component
@HandleClientPacket(type = PacketType.OSU_USER_STATS_REQUEST, checkForRestriction = true)
class UserStatsRequestHandler(private val presenceService: PresenceService) :
    AbstractPacketHandler<UserStatsRequestPacket>(PacketType.OSU_USER_STATS_REQUEST) {
    override fun handle(
        packet: UserStatsRequestPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        presenceService.handleUserStatsRequest(packet, responseStream)
    }
}
