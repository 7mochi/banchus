package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class ChannelJoinPacket : BanchoPacket(), BanchoPacket.Client {
    override val type = PacketType.OSU_CHANNEL_JOIN

    var name: String = ""

    override fun read(reader: DataReader, ins: InputStream) {
        this.name = reader.readString(ins)
    }
}
