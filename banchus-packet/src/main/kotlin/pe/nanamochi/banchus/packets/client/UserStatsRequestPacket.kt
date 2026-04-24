package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.core.ClientPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class UserStatsRequestPacket : ClientPacket() {
    override val type = PacketType.OSU_USER_STATS_REQUEST

    var userIds: List<Int> = emptyList()

    override fun read(reader: DataReader, ins: InputStream) {
        this.userIds = reader.readIntList16(ins).toList()
    }
}
