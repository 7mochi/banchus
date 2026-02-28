package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class MatchJoinPacket : BanchoPacket(), BanchoPacket.Client {
    override val type = PacketType.OSU_MATCH_JOIN

    var matchId: Int = 0
    var matchPassword: String? = null

    override fun read(reader: DataReader, ins: InputStream) {
        this.matchId = reader.readInt32(ins)
        this.matchPassword = reader.readString(ins)
    }
}
