package pe.nanamochi.banchus.protocol

import java.io.ByteArrayOutputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session

abstract class AbstractPacketHandler<T : BanchoPacket.Client>(val type: PacketType) {
    abstract fun handle(packet: T, session: Session, responseStream: ByteArrayOutputStream)
}
