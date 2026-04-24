package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.core.ClientPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class PresenceRequestAllPacket : ClientPacket() {
    override val type = PacketType.OSU_PRESENCE_REQUEST_ALL

    override fun read(reader: DataReader, ins: InputStream) {}
}
