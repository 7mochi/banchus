package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataWriter

class MessagePacket(
    var sender: String = "",
    var content: String = "",
    var target: String = "",
    var senderId: Int = 0,
) : BanchoPacket(), BanchoPacket.Server {
    override val type = PacketType.BANCHO_MESSAGE

    override fun write(writer: DataWriter, out: OutputStream) {
        writer.writeString(out, sender)
        writer.writeString(out, content)
        writer.writeString(out, target)
        writer.writeInt32(out, senderId)
    }
}
