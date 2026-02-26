package pe.nanamochi.banchus.handlers

import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.dto.ScoreUpdateData
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchScoreUpdatePacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_SCORE_UPDATE)
class MatchScoreUpdateHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchScoreUpdatePacket>(PacketType.OSU_MATCH_SCORE_UPDATE) {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MatchScoreUpdatePacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        val frame = packet.frame

        val data =
            ScoreUpdateData(
                time = frame.time,
                total300 = frame.total300,
                total100 = frame.total100,
                total50 = frame.total50,
                totalGeki = frame.totalGeki,
                totalKatu = frame.totalKatu,
                totalMiss = frame.totalMiss,
                totalScore = frame.totalScore,
                maxCombo = frame.maxCombo,
                currentCombo = frame.currentCombo,
                perfect = frame.perfect,
                hp = frame.hp,
                tagByte = frame.tagByte,
                usingScoreV2 = frame.usingScoreV2,
                comboPortion = frame.comboPortion,
                bonusPortion = frame.bonusPortion,
            )
    }
}
