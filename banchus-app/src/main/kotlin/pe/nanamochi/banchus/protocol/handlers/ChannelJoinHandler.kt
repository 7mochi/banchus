package pe.nanamochi.banchus.protocol.handlers

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.ChannelJoinPacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.service.ChannelService

@Component
@HandleClientPacket(type = PacketType.OSU_CHANNEL_JOIN, checkForRestriction = true)
class ChannelJoinHandler(private val channelService: ChannelService) :
    AbstractPacketHandler<ChannelJoinPacket>(PacketType.OSU_CHANNEL_JOIN) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: ChannelJoinPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        channelService.joinChannel(session, packet.name).onFailure { error ->
            log.warn("Failed join attempt for user {}: {}", session.user?.username, error)
        }
    }
}
