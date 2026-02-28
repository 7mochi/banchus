package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class MessagePacket : BanchoPacket(), BanchoPacket.Client {
    override val type = PacketType.OSU_MESSAGE

    var sender: String = ""
    var content: String = ""
    var target: String = ""
    var senderId: Int = 0

    override fun read(reader: DataReader, ins: InputStream) {
        this.sender = reader.readString(ins)
        this.content = reader.readString(ins)
        this.target = reader.readString(ins)
        this.senderId = reader.readInt32(ins)
    }
}
