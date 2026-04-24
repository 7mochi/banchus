package pe.nanamochi.banchus.handlers.ignored

import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.CantSpectatePacket
import pe.nanamochi.banchus.redis.entity.Session

@Component
@HandleClientPacket(type = PacketType.OSU_CANT_SPECTATE, checkForRestriction = true)
class CantSpectateHandler() :
    AbstractPacketHandler<CantSpectatePacket>(PacketType.OSU_CANT_SPECTATE) {
    override fun handle(
        packet: CantSpectatePacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {}
}
