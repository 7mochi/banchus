package pe.nanamochi.banchus.protocol.handlers

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.domain.enums.SlotStatus
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchNoBeatmapPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_NO_BEATMAP)
class MatchNoBeatmapHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchNoBeatmapPacket>(PacketType.OSU_MATCH_NO_BEATMAP) {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MatchNoBeatmapPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.changeSlotStatus(session, SlotStatus.NO_BEATMAP).onFailure { error ->
            log.warn("Failed to change slot status for user {}: {}", session.user?.username, error)
        }
    }
}
