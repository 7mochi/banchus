package pe.nanamochi.banchus.protocol.handlers

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
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
        multiplayerService.createMatchFromCreatePacket(session, packet).onFailure { error ->
            log.warn("Failed to create match for user {}: {}", session.user?.username, error)
        }
    }
}
