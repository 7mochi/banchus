package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.components.User
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.io.DataWriter

class UserStatsPacket(var user: User = User()) : ServerPacket() {
    override val type = PacketType.BANCHO_USER_STATS

    override fun write(writer: DataWriter, out: OutputStream) {
        user.writeStats(writer, out)
    }
}
