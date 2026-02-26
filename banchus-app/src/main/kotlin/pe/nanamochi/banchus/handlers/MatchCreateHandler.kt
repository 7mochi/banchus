package pe.nanamochi.banchus.handlers

import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.domain.enums.MatchTeamType
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.domain.enums.ScoringType
import pe.nanamochi.banchus.dto.MatchCreateData
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchCreatePacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_CREATE)
class MatchCreateHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchCreatePacket>(PacketType.OSU_MATCH_CREATE) {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MatchCreatePacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        val match = packet.match

        val data =
            MatchCreateData(
                name = match.name,
                password = match.password,
                beatmapName = match.beatmapName,
                beatmapId = match.beatmapId,
                beatmapMd5 = match.beatmapMd5,
                mode = Mode.fromValue(match.mode.value),
                mods = match.mods,
                scoringType = ScoringType.fromValue(match.scoringType.value),
                teamType = MatchTeamType.fromValue(match.teamType.value),
                freemodsEnabled = match.freemodsEnabled,
                randomSeed = match.randomSeed.toInt(),
            )
    }
}
