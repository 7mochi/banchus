package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataWriter

class MatchJoinFailPacket : BanchoPacket(), BanchoPacket.Server {
    override val type = PacketType.BANCHO_MATCH_JOIN_FAIL

    override fun write(writer: DataWriter, out: OutputStream) {}
}
