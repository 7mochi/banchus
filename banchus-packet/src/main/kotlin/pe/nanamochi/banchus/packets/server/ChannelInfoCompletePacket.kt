package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.io.DataWriter

class ChannelInfoCompletePacket : ServerPacket() {
    override val type = PacketType.BANCHO_CHANNEL_INFO_COMPLETE

    override fun write(writer: DataWriter, out: OutputStream) {}
}
