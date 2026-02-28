package pe.nanamochi.banchus.protocol.handlers

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.LobbyPartPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.ChannelService
import pe.nanamochi.banchus.service.SessionService

@Component
@HandleClientPacket(type = PacketType.OSU_LOBBY_PART)
class LobbyPartHandler(
    private val sessionService: SessionService,
    private val channelService: ChannelService,
) : AbstractPacketHandler<LobbyPartPacket>(PacketType.OSU_LOBBY_PART) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: LobbyPartPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        binding {
                session.receiveMatchUpdates = false
                sessionService.update(session).bind()
                channelService.leaveChannel(session, "#lobby").bind()
            }
            .onFailure { error ->
                log.warn("Lobby part failed for user {}: {}", session.user?.username, error)
            }
    }
}
