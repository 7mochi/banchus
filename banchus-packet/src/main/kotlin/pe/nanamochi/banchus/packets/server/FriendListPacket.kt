package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.io.DataWriter

class FriendListPacket(var friends: List<Int>) : ServerPacket() {
    override val type = PacketType.BANCHO_FRIENDS_LIST

    override fun write(writer: DataWriter, out: OutputStream) {
        writer.writeIntList16(out, friends)
    }
}
