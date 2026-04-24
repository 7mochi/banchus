package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.components.Match
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.io.DataWriter

class NewMatchPacket(var match: Match? = null, var shouldSendPassword: Boolean = false) :
    ServerPacket() {
    override val type = PacketType.BANCHO_NEW_MATCH

    override fun write(writer: DataWriter, out: OutputStream) {
        match?.write(writer, out, shouldSendPassword)
    }
}
