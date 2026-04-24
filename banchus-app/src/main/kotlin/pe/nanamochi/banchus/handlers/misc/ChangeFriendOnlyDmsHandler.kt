package pe.nanamochi.banchus.handlers.misc

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.ChangeFriendOnlyDmsPacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.SessionService

@Component
@HandleClientPacket(type = PacketType.OSU_CHANGE_FRIEND_ONLY_DMS)
class ChangeFriendOnlyDmsHandler(private val sessionService: SessionService) :
    AbstractPacketHandler<ChangeFriendOnlyDmsPacket>(PacketType.OSU_CHANGE_FRIEND_ONLY_DMS) {
    override fun handle(
        packet: ChangeFriendOnlyDmsPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        sessionService.setPrivateDms(session, packet.enabled)
    }
}
