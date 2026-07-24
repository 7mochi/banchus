package pe.nanamochi.banchus.auth.handlers

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.auth.service.SessionService
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.ChangeFriendOnlyDmsPacket

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
