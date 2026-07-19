package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.components.QuitState
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.io.DataWriter

class UserQuitPacket(var userId: Int = 0, var state: QuitState = QuitState.GONE) : ServerPacket() {
    override val type = PacketType.BANCHO_USER_QUIT

    override fun write(writer: DataWriter, out: OutputStream) {
        writer.writeInt32(out, userId)
        writer.writeUint8(out, state.value.toUByte())
    }
}
