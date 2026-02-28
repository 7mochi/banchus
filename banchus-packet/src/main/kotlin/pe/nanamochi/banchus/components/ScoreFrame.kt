package pe.nanamochi.banchus.components

import java.io.InputStream
import java.io.OutputStream
import pe.nanamochi.banchus.io.DataReader
import pe.nanamochi.banchus.io.DataWriter

data class ScoreFrame(
    var time: Int = 0,
    var id: Int = 0,
    var total300: Int = 0,
    var total100: Int = 0,
    var total50: Int = 0,
    var totalGeki: Int = 0,
    var totalKatu: Int = 0,
    var totalMiss: Int = 0,
    var totalScore: Int = 0,
    var maxCombo: Int = 0,
    var currentCombo: Int = 0,
    var perfect: Boolean = false,
    var hp: Int = 0,
    var tagByte: Int = 0,
    var usingScoreV2: Boolean = false,
    var comboPortion: Double = 0.0,
    var bonusPortion: Double = 0.0,
) {
    fun write(writer: DataWriter, stream: OutputStream) {
        writer.writeInt32(stream, time)
        writer.writeUint8(stream, id.toUByte())
        writer.writeUint16(stream, total300.toUShort())
        writer.writeUint16(stream, total100.toUShort())
        writer.writeUint16(stream, total50.toUShort())
        writer.writeUint16(stream, totalGeki.toUShort())
        writer.writeUint16(stream, totalKatu.toUShort())
        writer.writeUint16(stream, totalMiss.toUShort())
        writer.writeUint32(stream, totalScore.toUInt())
        writer.writeUint16(stream, maxCombo.toUShort())
        writer.writeUint16(stream, currentCombo.toUShort())
        writer.writeBoolean(stream, perfect)
        writer.writeUint8(stream, hp.toUByte())
        writer.writeUint8(stream, tagByte.toUByte())
        writer.writeBoolean(stream, usingScoreV2)

        if (usingScoreV2) {
            writer.writeFloat64(stream, comboPortion)
            writer.writeFloat64(stream, bonusPortion)
        }
    }

    companion object {
        fun read(reader: DataReader, ins: InputStream): ScoreFrame =
            ScoreFrame().apply {
                time = reader.readInt32(ins)
                id = reader.readUint8(ins).toInt()
                total300 = reader.readUint16(ins).toInt()
                total100 = reader.readUint16(ins).toInt()
                total50 = reader.readUint16(ins).toInt()
                totalGeki = reader.readUint16(ins).toInt()
                totalKatu = reader.readUint16(ins).toInt()
                totalMiss = reader.readUint16(ins).toInt()
                totalScore = reader.readUint32(ins).toInt()
                maxCombo = reader.readUint16(ins).toInt()
                currentCombo = reader.readUint16(ins).toInt()
                perfect = reader.readUint8(ins) == 1.toUByte()
                hp = reader.readUint8(ins).toInt()
                tagByte = reader.readUint8(ins).toInt()
                usingScoreV2 = reader.readUint8(ins) == 1.toUByte()

                if (usingScoreV2) {
                    comboPortion = reader.readFloat64(ins)
                    bonusPortion = reader.readFloat64(ins)
                }
            }
    }
}
