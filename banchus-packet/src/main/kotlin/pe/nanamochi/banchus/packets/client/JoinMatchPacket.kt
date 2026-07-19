package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.core.ClientPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class JoinMatchPacket : ClientPacket() {
    override val type = PacketType.OSU_JOIN_MATCH

    var matchId: Int = 0
    var matchPassword: String? = null

    override fun read(reader: DataReader, ins: InputStream) {
        this.matchId = reader.readInt32(ins)
        this.matchPassword = reader.readString(ins)
    }
}
