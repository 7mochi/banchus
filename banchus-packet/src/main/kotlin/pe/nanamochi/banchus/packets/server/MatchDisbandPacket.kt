package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.io.DataWriter

class MatchDisbandPacket(var matchId: Int = 0) : ServerPacket() {
    override val type = PacketType.BANCHO_MATCH_DISBAND

    override fun write(writer: DataWriter, out: OutputStream) {
        writer.writeUint32(out, matchId.toUInt())
    }
}
