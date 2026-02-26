package pe.nanamochi.banchus.handlers

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
        session.receiveMatchUpdates = false
    }
}
