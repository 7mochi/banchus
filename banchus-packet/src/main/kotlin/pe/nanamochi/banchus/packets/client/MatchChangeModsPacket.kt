package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class MatchChangeModsPacket : BanchoPacket(), BanchoPacket.Client {
    override val type = PacketType.OSU_MATCH_CHANGE_MODS

    var mods: UInt = 0u

    override fun read(reader: DataReader, ins: InputStream) {
        this.mods = reader.readUint32(ins)
    }
}
