package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataWriter

class MatchPlayerFailedPacket(var slotId: Int = 0) : BanchoPacket(), BanchoPacket.Server {
    override val type = PacketType.BANCHO_MATCH_PLAYER_FAILED

    override fun write(writer: DataWriter, out: OutputStream) {
        writer.writeInt32(out, slotId)
    }
}
