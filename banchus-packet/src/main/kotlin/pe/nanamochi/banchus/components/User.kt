package pe.nanamochi.banchus.components

import java.io.OutputStream
import pe.nanamochi.banchus.io.DataWriter

data class User(
    var id: Int = 0,
    var username: String = "",
    var presence: UserPresence = UserPresence(),
    var stats: UserStats = UserStats(),
    var status: UserStatus = UserStatus(),
    var globalRank: Int = 0,
) {
    fun write(writer: DataWriter, out: OutputStream) {
        writer.writeInt32(out, id)
        writer.writeString(out, username)
        writer.writeUint8(out, presence.utcOffset)
        writer.writeUint8(out, presence.country)
        writer.writeUint8(out, presence.permissions)
        writer.writeFloat32(out, presence.latitude)
        writer.writeFloat32(out, presence.longitude)
        writer.writeInt32(out, globalRank)
    }
}
