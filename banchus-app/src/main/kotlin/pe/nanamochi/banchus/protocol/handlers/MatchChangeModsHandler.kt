package pe.nanamochi.banchus.protocol.handlers

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MatchChangeModsPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.MultiplayerService

@Component
@HandleClientPacket(type = PacketType.OSU_MATCH_CHANGE_MODS)
class MatchChangeModsHandler(private val multiplayerService: MultiplayerService) :
    AbstractPacketHandler<MatchChangeModsPacket>(PacketType.OSU_MATCH_CHANGE_MODS) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MatchChangeModsPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        multiplayerService.changeMods(session, packet.mods.toInt()).onFailure { error ->
            log.warn("Failed to change match mods for user {}: {}", session.user?.username, error)
        }
    }
}
