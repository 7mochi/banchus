package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.components.Match
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.io.DataWriter

class MatchStartPacket(var match: Match? = null, var shouldSendPassword: Boolean = false) :
    ServerPacket() {
    override val type = PacketType.BANCHO_MATCH_START

    override fun write(writer: DataWriter, out: OutputStream) {
        match?.let { writeMatch(writer, out, it, shouldSendPassword) }
    }

    private fun writeMatch(
        writer: DataWriter,
        out: OutputStream,
        match: Match,
        shouldSendPassword: Boolean,
    ) {
        match.write(writer, out, shouldSendPassword)
    }
}
