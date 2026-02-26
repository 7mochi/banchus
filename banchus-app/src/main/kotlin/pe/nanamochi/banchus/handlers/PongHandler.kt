package pe.nanamochi.banchus.handlers

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.PongPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler

@Component
@HandleClientPacket(type = PacketType.OSU_PONG, checkForRestriction = true)
class PongHandler : AbstractPacketHandler<PongPacket>(PacketType.OSU_PONG) {

    override fun handle(
        packet: PongPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {}
}
