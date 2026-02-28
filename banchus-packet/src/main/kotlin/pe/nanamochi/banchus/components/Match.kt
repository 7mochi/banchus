package pe.nanamochi.banchus.components

import java.io.InputStream
import java.io.OutputStream
import pe.nanamochi.banchus.io.DataReader
import pe.nanamochi.banchus.io.DataWriter

data class Match(
    var id: Int = 0,
    var inProgress: Boolean = false,
    var type: MatchType = MatchType.STANDARD,
    var mods: UInt = 0u,
    var name: String = "",
    var password: String? = null,
    var beatmapName: String = "",
    var beatmapId: Int = 0,
    var beatmapMd5: String = "",
    var slots: List<MatchSlot> = List(16) { MatchSlot() },
    var hostId: Int = 0,
    var mode: Mode = Mode.OSU,
    var scoringType: ScoringType = ScoringType.SCORE,
    var teamType: MatchTeamType = MatchTeamType.HEAD_TO_HEAD,
    var freemodsEnabled: Boolean = false,
    var randomSeed: UInt = 0u,
) {
    fun write(writer: DataWriter, out: OutputStream, shouldSendPassword: Boolean) {
        writer.writeUint16(out, id.toUShort())
        writer.writeBoolean(out, inProgress)
        writer.writeUint8(out, MatchType.STANDARD.value.toUByte())
        writer.writeUint32(out, mods)
        writer.writeString(out, name)

        if (!password.isNullOrEmpty()) {
            if (shouldSendPassword) {
                writer.writeString(out, password!!)
            } else {
                out.write(byteArrayOf(0x0B, 0x00))
            }
        } else {
            out.write(byteArrayOf(0x00))
        }

        writer.writeString(out, beatmapName)
        writer.writeInt32(out, beatmapId)
        writer.writeString(out, beatmapMd5)

        slots.forEach { writer.writeUint8(out, it.status.toUByte()) }
        slots.forEach { writer.writeUint8(out, it.team.value.toUByte()) }

        slots.forEach { slot ->
            if ((slot.status.toInt() and SlotStatus.HAS_PLAYER) != 0) {
                writer.writeInt32(out, slot.userId)
            }
        }

        writer.writeInt32(out, hostId)
        writer.writeUint8(out, mode.value)
        writer.writeUint8(out, scoringType.value.toUByte())
        writer.writeUint8(out, teamType.value.toUByte())
        writer.writeBoolean(out, freemodsEnabled)

        if (freemodsEnabled) {
            slots.forEach { writer.writeUint32(out, it.mods) }
        }
        writer.writeUint32(out, randomSeed)
    }

    companion object {
        fun read(reader: DataReader, ins: InputStream): Match {
            val match = Match()

            match.id = reader.readUint16(ins).toInt()
            match.inProgress = reader.readBoolean(ins)
            match.type = MatchType.fromValue(reader.readUint8(ins).toInt())
            match.mods = reader.readUint32(ins)
            match.name = reader.readString(ins)
            match.password = reader.readString(ins)
            match.beatmapName = reader.readString(ins)
            match.beatmapId = reader.readInt32(ins)
            match.beatmapMd5 = reader.readString(ins)

            match.slots.forEach { it.status = reader.readUint8(ins).toByte() }

            match.slots.forEach { it.team = SlotTeam.fromValue(reader.readUint8(ins).toInt()) }

            match.slots.forEach { slot ->
                if ((slot.status.toInt() and SlotStatus.HAS_PLAYER) != 0) {
                    slot.userId = reader.readInt32(ins)
                }
            }

            match.hostId = reader.readInt32(ins)
            match.mode = Mode.fromValue(reader.readUint8(ins))
            match.scoringType = ScoringType.fromValue(reader.readUint8(ins).toInt())
            match.teamType = MatchTeamType.fromValue(reader.readUint8(ins).toInt())
            match.freemodsEnabled = reader.readBoolean(ins)

            if (match.freemodsEnabled) {
                match.slots.forEach { it.mods = reader.readUint32(ins) }
            }

            match.randomSeed = reader.readUint32(ins)

            return match
        }
    }
}
