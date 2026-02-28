package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.components.Mods
import pe.nanamochi.banchus.components.User
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataWriter

class UserStatsPacket(var user: User = User()) : BanchoPacket(), BanchoPacket.Server {
    override val type = PacketType.BANCHO_USER_STATS

    override fun write(writer: DataWriter, out: OutputStream) {
        writer.writeInt32(out, user.id)
        writer.writeUint8(out, user.status.action)
        writer.writeString(out, user.status.infoText)
        writer.writeString(out, user.status.beatmapMd5)
        writer.writeUint32(out, Mods.toBitmask(user.status.mods))
        writer.writeUint8(out, user.status.gamemode.value)
        writer.writeInt32(out, user.status.beatmapId)
        writer.writeUint64(out, user.stats.rankedScore)
        writer.writeFloat32(out, user.stats.accuracy / 100.0f)
        writer.writeUint32(out, user.stats.playCount)
        writer.writeUint64(out, user.stats.totalScore)
        writer.writeUint32(out, user.stats.globalRank)
        writer.writeUint16(out, user.stats.performancePoints)
    }
}
