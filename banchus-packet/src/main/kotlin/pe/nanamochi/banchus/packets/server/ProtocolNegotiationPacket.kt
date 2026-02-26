package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataWriter

class ProtocolNegotiationPacket(var protocolVersion: Int = 19) :
    BanchoPacket(), BanchoPacket.Server {
    override val type = PacketType.BANCHO_PROTOCOL_NEGOTIATION

    override fun write(writer: DataWriter, out: OutputStream) {
        writer.writeInt32(out, protocolVersion)
    }
}
