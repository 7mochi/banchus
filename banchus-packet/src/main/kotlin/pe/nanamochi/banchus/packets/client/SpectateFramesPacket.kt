package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.components.ReplayFrameBundle
import pe.nanamochi.banchus.core.ClientPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class SpectateFramesPacket : ClientPacket() {
    override val type = PacketType.OSU_SPECTATE_FRAMES

    var replayFrameBundle: ReplayFrameBundle = ReplayFrameBundle()

    override fun read(reader: DataReader, ins: InputStream) {
        this.replayFrameBundle = ReplayFrameBundle.read(reader, ins)
    }
}
