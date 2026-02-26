package pe.nanamochi.banchus.handlers

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
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
        // #lobby has its own handler
        if (packet.name == "#lobby" && session.receiveMatchUpdates) {
            return
        }

        channelService
            .leaveChannel(session, packet.name)
            .onSuccess {
                log.info("User {} has left channel {}.", session.user?.username, packet.name)
            }
            .onFailure { error ->
                log.warn(
                    "Failed leave attempt for user {} from channel: {}",
                    session.user?.username,
                    error,
                )
            }
    }
}
