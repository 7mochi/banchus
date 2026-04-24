package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.io.DataWriter

class PingPacket : ServerPacket() {
    override val type = PacketType.BANCHO_PING

    override fun write(writer: DataWriter, out: OutputStream) {}
}
