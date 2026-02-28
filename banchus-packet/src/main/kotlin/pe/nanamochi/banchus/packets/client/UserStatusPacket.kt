package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.components.Mode
import pe.nanamochi.banchus.components.Mods
import pe.nanamochi.banchus.components.Status
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class UserStatusPacket : BanchoPacket(), BanchoPacket.Client {
    override val type = PacketType.OSU_USER_STATUS

    var action: Status = Status.IDLE
    var text: String = ""
    var beatmapChecksum: String = ""
    var mods: List<Mods> = emptyList()
    var mode: Mode = Mode.OSU
    var beatmapId: Int = 0

    override fun read(reader: DataReader, ins: InputStream) {
        this.action = Status.fromValue(reader.readUint8(ins).toInt())
        this.text = reader.readString(ins)
        this.beatmapChecksum = reader.readString(ins)
        this.mods = Mods.fromBitmask(reader.readUint32(ins))
        this.mode = Mode.fromValue(reader.readUint8(ins))
        this.beatmapId = reader.readInt32(ins)
    }
}
