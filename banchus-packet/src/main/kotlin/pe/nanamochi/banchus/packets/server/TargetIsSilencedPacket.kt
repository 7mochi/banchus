package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataWriter

class TargetIsSilencedPacket(var username: String = "") : BanchoPacket(), BanchoPacket.Server {
    override val type = PacketType.BANCHO_TARGET_IS_SILENCED

    override fun write(writer: DataWriter, out: OutputStream) {
        val messagePacket =
            MessagePacket(sender = "", content = "", target = username, senderId = -1)
        messagePacket.write(writer, out)
    }
}
