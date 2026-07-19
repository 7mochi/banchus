package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.core.ClientPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class TournamentMatchInfoPacket : ClientPacket() {
    override val type = PacketType.OSU_TOURNAMENT_MATCH_INFO

    var matchId: Int = 0

    override fun read(reader: DataReader, ins: InputStream) {
        this.matchId = reader.readInt32(ins)
    }
}
