package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataWriter

class AnnouncePacket(var message: String = "") : BanchoPacket(), BanchoPacket.Server {
    override val type = PacketType.BANCHO_ANNOUNCE

    override fun write(writer: DataWriter, out: OutputStream) {
        writer.writeString(out, message)
    }
}
