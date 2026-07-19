package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.components.Match
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.io.DataWriter

class MatchUpdatePacket(var match: Match? = null, var shouldSendPassword: Boolean = false) :
    ServerPacket() {
    override val type = PacketType.BANCHO_MATCH_UPDATE

    override fun write(writer: DataWriter, out: OutputStream) {
        match?.write(writer, out, shouldSendPassword)
    }
}
