package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class StatusUpdateRequestPacket : BanchoPacket(), BanchoPacket.Client {
    override val type = PacketType.OSU_STATUS_UPDATE_REQUEST

    override fun read(reader: DataReader, ins: InputStream) {}
}
