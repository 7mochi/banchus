package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.core.ClientPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class ChangeFriendOnlyDmsPacket : ClientPacket() {
    override val type = PacketType.OSU_CHANGE_FRIEND_ONLY_DMS

    var enabled: Boolean = false

    override fun read(reader: DataReader, ins: InputStream) {
        this.enabled = reader.readInt32(ins) == 1
    }
}
