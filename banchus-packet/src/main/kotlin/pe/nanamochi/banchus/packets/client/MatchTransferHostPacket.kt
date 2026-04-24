package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.core.ClientPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class MatchTransferHostPacket : ClientPacket() {
    override val type = PacketType.OSU_MATCH_TRANSFER_HOST

    var slotId: Int = 0

    override fun read(reader: DataReader, ins: InputStream) {
        this.slotId = reader.readInt32(ins)
    }
}
