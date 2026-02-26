package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.components.ReplayFrameBundle
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataWriter

class SpectateFramesPacket(var replayFrameBundle: ReplayFrameBundle? = null) :
    BanchoPacket(), BanchoPacket.Server {
    override val type = PacketType.BANCHO_SPECTATE_FRAMES

    override fun write(writer: DataWriter, out: OutputStream) {
        val bundle = replayFrameBundle ?: return

        writer.writeUint32(out, bundle.extra.toUInt())
        writer.writeUint16(out, bundle.frames.size.toUShort())

        for (frame in bundle.frames) {
            writer.writeUint8(out, frame.buttonState.toUByte())
            writer.writeUint8(out, frame.taikoByte.toUByte())
            writer.writeFloat32(out, frame.x)
            writer.writeFloat32(out, frame.y)
            writer.writeInt32(out, frame.time)
        }

        writer.writeUint8(out, bundle.action.value.toUByte())

        bundle.frame?.write(writer, out)

        writer.writeUint16(out, bundle.sequence.toUShort())
    }
}
