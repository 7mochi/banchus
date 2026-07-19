package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.io.DataWriter

class ChannelAvailablePacket(
    var realName: String = "",
    var topic: String = "",
    var userCount: Int = 0,
) : ServerPacket() {
    override val type = PacketType.BANCHO_CHANNEL_AVAILABLE

    override fun write(writer: DataWriter, out: OutputStream) {
        writer.writeString(out, realName)
        writer.writeString(out, topic)
        writer.writeInt32(out, userCount)
    }
}
