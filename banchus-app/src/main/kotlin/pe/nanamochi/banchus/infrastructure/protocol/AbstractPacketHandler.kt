package pe.nanamochi.banchus.infrastructure.protocol

import java.io.ByteArrayOutputStream
import pe.nanamochi.banchus.core.ClientPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.redis.entity.Session

abstract class AbstractPacketHandler<T : ClientPacket>(val type: PacketType) {
    abstract fun handle(packet: T, session: Session, responseStream: ByteArrayOutputStream)
}
