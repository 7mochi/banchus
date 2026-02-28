package pe.nanamochi.banchus.protocol.handlers

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.ChannelLeavePacket
import pe.nanamochi.banchus.protocol.AbstractPacketHandler
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
        if (
            (packet.name == "#lobby" && session.receiveMatchUpdates) ||
                packet.name == "#multiplayer" ||
                packet.name == "#spectator"
        ) {
            return
        }

        channelService.leaveChannel(session, packet.name).onFailure { error ->
            log.warn(
                "Failed leave attempt for user {} from channel: {}",
                session.user?.username,
                error,
            )
        }
    }
}
