package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.core.ClientPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class ChannelJoinPacket : ClientPacket() {
    override val type = PacketType.OSU_CHANNEL_JOIN

    var name: String = ""

    override fun read(reader: DataReader, ins: InputStream) {
        this.name = reader.readString(ins)
    }
}
