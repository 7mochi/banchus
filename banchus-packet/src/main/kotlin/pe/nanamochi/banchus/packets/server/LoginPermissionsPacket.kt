package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataWriter

class LoginPermissionsPacket(var privileges: Int = 0) : BanchoPacket(), BanchoPacket.Server {
    override val type = PacketType.BANCHO_LOGIN_PERMISSIONS

    override fun write(writer: DataWriter, out: OutputStream) {
        writer.writeInt32(out, privileges)
    }
}
