package pe.nanamochi.banchus.packets.server

import java.io.OutputStream
import pe.nanamochi.banchus.components.ScoreFrame
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataWriter

class MatchScoreUpdatePacket(var frame: ScoreFrame? = null) : BanchoPacket(), BanchoPacket.Server {
    override val type = PacketType.BANCHO_MATCH_SCORE_UPDATE

    override fun write(writer: DataWriter, out: OutputStream) {
        frame?.write(writer, out)
    }
}
