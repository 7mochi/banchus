package pe.nanamochi.banchus.packets.client

import java.io.InputStream
import pe.nanamochi.banchus.components.ScoreFrame
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.DataReader

class MatchScoreUpdatePacket : BanchoPacket(), BanchoPacket.Client {
    override val type = PacketType.OSU_MATCH_SCORE_UPDATE

    var frame: ScoreFrame = ScoreFrame()

    override fun read(reader: DataReader, ins: InputStream) {
        this.frame = ScoreFrame.read(reader, ins)
    }
}
