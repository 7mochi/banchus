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
    fun writePresence(writer: DataWriter, out: OutputStream) {
        writer.writeInt32(out, id)
        writer.writeString(out, username)
        writer.writeUint8(out, (presence.utcOffset + 24u).toUByte())
        writer.writeUint8(out, presence.country)
        writer.writeUint8(out, presence.permissions)
        writer.writeFloat32(out, presence.longitude)
        writer.writeFloat32(out, presence.latitude)
        writer.writeInt32(out, globalRank)
        writer.writeUint8(out, status.gamemode.value)
    }

    fun writeStats(writer: DataWriter, out: OutputStream) {
        writer.writeInt32(out, id)
        writer.writeUint8(out, status.action)
        writer.writeString(out, status.infoText)
        writer.writeString(out, status.beatmapMd5)
        writer.writeUint32(out, Mods.toBitmask(status.mods))
        writer.writeUint8(out, status.gamemode.value)
        writer.writeInt32(out, status.beatmapId)
        writer.writeUint64(out, stats.rankedScore)
        writer.writeFloat32(out, stats.accuracy / 100.0f)
        writer.writeUint32(out, stats.playCount)
        writer.writeUint64(out, stats.totalScore)
        writer.writeUint32(out, stats.globalRank)
        writer.writeUint16(out, stats.performancePoints)
    }
}
