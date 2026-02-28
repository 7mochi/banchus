package pe.nanamochi.banchus.protocol.handlers

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchChangePasswordPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_CHANGE_PASSWORD)
class MatchChangePasswordHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchChangePasswordPacket>(PacketType.OSU_MATCH_CHANGE_PASSWORD) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MatchChangePasswordPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.changePassword(session, packet.match.password).onFailure { error ->
            log.warn("Failed to change match mods for user {}: {}", session.user?.username, error)
        }
    }
}
