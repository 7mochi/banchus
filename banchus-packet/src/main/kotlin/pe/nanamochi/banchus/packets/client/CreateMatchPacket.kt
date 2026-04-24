package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.components.Match
import pe.nanamochi.banchus.core.ClientPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class CreateMatchPacket : ClientPacket() {
    override val type = PacketType.OSU_CREATE_MATCH

    var match: Match = Match()

    override fun read(reader: DataReader, ins: InputStream) {
        this.match = Match.read(reader, ins)
    }
}
