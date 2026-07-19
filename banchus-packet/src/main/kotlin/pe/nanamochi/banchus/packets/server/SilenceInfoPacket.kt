package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.io.DataWriter

class SilenceInfoPacket(var silenceLength: Int = 0) : ServerPacket() {
    override val type = PacketType.BANCHO_SILENCE_INFO

    override fun write(writer: DataWriter, out: OutputStream) {
        writer.writeInt32(out, silenceLength)
    }
}
