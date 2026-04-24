package pe.nanamochi.banchus.handlers.ignored

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.PongPacket
import pe.nanamochi.banchus.redis.entity.Session

@Component
@HandleClientPacket(type = PacketType.OSU_PONG, checkForRestriction = true)
class PongHandler : AbstractPacketHandler<PongPacket>(PacketType.OSU_PONG) {
    override fun handle(
        packet: PongPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {}
}
