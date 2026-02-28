package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class MatchNotReadyPacket : BanchoPacket(), BanchoPacket.Client {
    override val type = PacketType.OSU_MATCH_NOT_READY

    override fun read(reader: DataReader, ins: InputStream) {}
}
