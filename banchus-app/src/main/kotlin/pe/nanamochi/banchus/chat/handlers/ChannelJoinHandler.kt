package pe.nanamochi.banchus.chat.handlers

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.chat.service.ChannelService
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.ChannelJoinPacket
import pe.nanamochi.banchus.packets.server.ChannelJoinSuccessPacket
import pe.nanamochi.banchus.protocol.PacketWriter

@Component
@HandleClientPacket(type = PacketType.OSU_CHANNEL_JOIN, checkForRestriction = true)
class ChannelJoinHandler(
    private val channelService: ChannelService,
    private val packetWriter: PacketWriter,
) : AbstractPacketHandler<ChannelJoinPacket>(PacketType.OSU_CHANNEL_JOIN) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: ChannelJoinPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        val channelName = packet.name
        if (channelName == "#highlight" || channelName == "#userlog") return

        binding {
                val internalName = channelService.getChannelName(session, channelName).bind()
                channelService.join(session, internalName).bind()
                responseStream.write(packetWriter.serialize(ChannelJoinSuccessPacket(packet.name)))
            }
            .onFailure { error ->
                log.warn(
                    "Failed join attempt for user {} to {}: {}",
                    session.username,
                    channelName,
                    error,
                )
            }
    }
}
