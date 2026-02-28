package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataWriter

class MatchAllPlayersLoadedPacket : BanchoPacket(), BanchoPacket.Server {
    override val type = PacketType.BANCHO_MATCH_ALL_PLAYERS_LOADED

    override fun write(writer: DataWriter, out: OutputStream) {}
}
