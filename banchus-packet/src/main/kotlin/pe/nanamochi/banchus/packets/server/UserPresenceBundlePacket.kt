package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.io.DataWriter

class UserPresenceBundlePacket(var userIds: List<Int>) : ServerPacket() {
    override val type = PacketType.BANCHO_USER_PRESENCE_BUNDLE

    override fun write(writer: DataWriter, out: OutputStream) {
        writer.writeIntList16(out, userIds)
    }
}
