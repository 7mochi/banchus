package pe.nanamochi.banchus.protocol.handlers

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchLoadCompletePacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_LOAD_COMPLETE)
class MatchLoadCompleteHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchLoadCompletePacket>(PacketType.OSU_MATCH_LOAD_COMPLETE) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MatchLoadCompletePacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.handleLoadComplete(session).onFailure { error ->
            log.warn(
                "Failed to handle load complete for match {}: {}",
                session.multiplayerMatchId,
                error,
            )
        }
    }
}
