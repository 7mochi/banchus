package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.io.DataWriter

class ChannelJoinSuccessPacket(var name: String = "") : ServerPacket() {
    override val type = PacketType.BANCHO_CHANNEL_JOIN_SUCCESS

    override fun write(writer: DataWriter, out: OutputStream) {
        writer.writeString(out, name)
    }
}
