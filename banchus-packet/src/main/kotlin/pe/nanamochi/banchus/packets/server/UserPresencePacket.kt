package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.components.User
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.io.DataWriter

class UserPresencePacket(var user: User = User()) : ServerPacket() {
    override val type = PacketType.BANCHO_USER_PRESENCE

    override fun write(writer: DataWriter, out: OutputStream) {
        user.writePresence(writer, out)
    }
}
