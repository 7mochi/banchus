package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.components.Mode
import pe.nanamochi.banchus.components.Mods
import pe.nanamochi.banchus.components.Status
import pe.nanamochi.banchus.components.StatusUpdate
import pe.nanamochi.banchus.core.ClientPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class ChangeStatusPacket : ClientPacket() {
    override val type = PacketType.OSU_CHANGE_STATUS

    var statusUpdate: StatusUpdate = StatusUpdate()

    override fun read(reader: DataReader, ins: InputStream) {
        this.statusUpdate.status = Status.fromValue(reader.readUint8(ins).toInt())
        this.statusUpdate.text = reader.readString(ins)
        this.statusUpdate.beatmapMd5 = reader.readString(ins)
        this.statusUpdate.mods = Mods.fromBitmask(reader.readUint32(ins))
        this.statusUpdate.mode = Mode.fromValue(reader.readUint8(ins))
        this.statusUpdate.beatmapId = reader.readInt32(ins)
    }
}
