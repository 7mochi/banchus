package pe.nanamochi.banchus.chat.handlers

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.chat.service.ChannelService
import pe.nanamochi.banchus.chat.service.ChatService
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MessagePacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.core.entity.Presence
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.service.StreamService

@Component
@HandleClientPacket(type = PacketType.OSU_MESSAGE, checkForRestriction = true)
class MessageHandler(
    private val chatService: ChatService,
    private val channelService: ChannelService,
    private val packetWriter: PacketWriter,
    private val streamService: StreamService,
) : AbstractPacketHandler<MessagePacket>(PacketType.OSU_MESSAGE) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MessagePacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        binding<Unit, DomainMessage> {
                val channelName = channelService.getChannelName(session, packet.target).bind()
                val messageStream = channelName.getMessageStream()

                val result =
                    chatService.sendChannelMessage(packet.content, packet.target, session).bind()

                val playerMessage =
                    pe.nanamochi.banchus.packets.server.MessagePacket(
                        sender = session.username,
                        content = result.message.content,
                        target = packet.target,
                        senderId = session.userId,
                    )
                streamService.broadcastData(
                    messageStream,
                    packetWriter.serialize(playerMessage),
                    listOf(session.sessionId),
                )

                if (result.response.isNotBlank()) {
                    streamService.broadcastData(
                        messageStream,
                        packetWriter.serialize(
                            pe.nanamochi.banchus.packets.server.MessagePacket(
                                sender = Presence.BOT_NAME,
                                content = result.response,
                                target = packet.target,
                                senderId = Presence.BOT_ID,
                            )
                        ),
                    )
                }
            }
            .onFailure { error ->
                log.warn("Message delivery failed for user {}: {}", session.username, error)
            }
    }
}
