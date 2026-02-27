package pe.nanamochi.banchus.protocol.handlers

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.UserStatsRequestPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler

@Component
@HandleClientPacket(type = PacketType.OSU_USER_STATS_REQUEST, checkForRestriction = true)
class UserStatsRequestHandler :
    AbstractPacketHandler<UserStatsRequestPacket>(PacketType.OSU_USER_STATS_REQUEST) {
    override fun handle(
        packet: UserStatsRequestPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {}
}
