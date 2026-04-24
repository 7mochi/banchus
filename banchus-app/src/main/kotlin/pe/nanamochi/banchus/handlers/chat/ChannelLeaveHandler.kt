package pe.nanamochi.banchus.handlers.chat

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.ChannelLeavePacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.ChannelService

@Component
@HandleClientPacket(type = PacketType.OSU_CHANNEL_LEAVE, checkForRestriction = true)
class ChannelLeaveHandler(private val channelService: ChannelService) :
    AbstractPacketHandler<ChannelLeavePacket>(PacketType.OSU_CHANNEL_LEAVE) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: ChannelLeavePacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        val channelName = packet.name

        if (channelName == "#highlight" || channelName == "#userlog") return
        if (!channelName.startsWith('#')) return

        binding {
                val internalName = channelService.getChannelName(session, channelName).bind()
                channelService.leave(session.sessionId, internalName).bind()
            }
            .onFailure { error ->
                log.warn("User {} failed to leave {}: {}", session.username, channelName, error)
            }
    }
}
