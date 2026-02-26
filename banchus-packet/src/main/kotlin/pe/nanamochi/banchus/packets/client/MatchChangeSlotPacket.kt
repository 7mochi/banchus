package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class MatchChangeSlotPacket : BanchoPacket(), BanchoPacket.Client {
    override val type = PacketType.OSU_MATCH_CHANGE_SLOT

    var slotId: Int = 0

    override fun read(reader: DataReader, ins: InputStream) {
        this.slotId = reader.readInt32(ins)
    }
}
