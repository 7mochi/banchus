package pe.nanamochi.banchus.components

import java.io.InputStream
import java.io.OutputStream
import pe.nanamochi.banchus.io.DataReader
import pe.nanamochi.banchus.io.DataWriter

data class ReplayFrameBundle(
    var extra: Int = 0,
    var frames: MutableList<ReplayFrame> = mutableListOf(),
    var action: ReplayAction = ReplayAction.STANDARD,
    var frame: ScoreFrame? = null,
    var sequence: Int = 0,
) {
    fun write(writer: DataWriter, stream: OutputStream) {
        writer.writeUint32(stream, extra.toUInt())
        writer.writeUint16(stream, frames.size.toUShort())

        for (replayFrame in frames) {
            writer.writeUint8(stream, replayFrame.buttonState.toUByte())
            writer.writeUint8(stream, replayFrame.taikoByte.toUByte())
            writer.writeFloat32(stream, replayFrame.x)
            writer.writeFloat32(stream, replayFrame.y)
            writer.writeInt32(stream, replayFrame.time)
        }

        writer.writeUint8(stream, action.value.toUByte())

        frame?.write(writer, stream)

        writer.writeUint16(stream, sequence.toUShort())
    }

    companion object {
        fun read(reader: DataReader, ins: InputStream): ReplayFrameBundle {
            val bundle = ReplayFrameBundle()

            bundle.extra = reader.readUint32(ins).toInt()
            val replayFrameCount = reader.readUint16(ins).toInt()

            val replayFrames = mutableListOf<ReplayFrame>()
            repeat(replayFrameCount) {
                replayFrames.add(
                    ReplayFrame().apply {
                        buttonState = reader.readUint8(ins).toInt()
                        taikoByte = reader.readUint8(ins).toInt()
                        x = reader.readFloat32(ins)
                        y = reader.readFloat32(ins)
                        time = reader.readInt32(ins)
                    }
                )
            }

            bundle.frames = replayFrames
            bundle.action = ReplayAction.fromValue(reader.readUint8(ins).toInt())
            bundle.frame = ScoreFrame.read(reader, ins)
            bundle.sequence = reader.readUint16(ins).toInt()

            return bundle
        }
    }
}
