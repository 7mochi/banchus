package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.core.ClientPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class AddFriendPacket : ClientPacket() {
    override val type = PacketType.OSU_ADD_FRIEND

    var userId: Int = 0

    override fun read(reader: DataReader, ins: InputStream) {
        this.userId = reader.readInt32(ins)
    }
}
