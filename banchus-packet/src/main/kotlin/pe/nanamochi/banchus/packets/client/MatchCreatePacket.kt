package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.components.Match
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class MatchCreatePacket : BanchoPacket(), BanchoPacket.Client {
    override val type = PacketType.OSU_MATCH_CREATE

    var match: Match = Match()

    override fun read(reader: DataReader, ins: InputStream) {
        this.match = Match.read(reader, ins)
    }
}
