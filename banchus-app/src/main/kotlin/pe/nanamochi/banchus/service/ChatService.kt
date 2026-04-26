package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Target
import pe.nanamochi.banchus.domain.error.DomainMessage
import pe.nanamochi.banchus.packets.client.MessagePacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.entity.Presence
import pe.nanamochi.banchus.redis.entity.Session

@Service
class ChatService(
    private val channelService: ChannelService,
    private val messageService: MessageService,
    private val streamService: StreamService,
    private val packetWriter: PacketWriter,
) {
    fun handleIncomingPublicChatMessage(
        packet: MessagePacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ): Result<Unit, DomainMessage> = binding {
        val channelName = channelService.getChannelName(session, packet.target).bind()
        val target = Target.Channel(channelName)

        val result = messageService.send(session, target, packet.content).bind()
        if (result.response.isNotEmpty() && result.response.isNotBlank()) {
            responseStream.write(
                packetWriter.serialize(
                    pe.nanamochi.banchus.packets.server.MessagePacket(
                        sender = Presence.BOT_NAME,
                        content = result.response, // TODO:3
                        target = packet.target,
                        senderId = Presence.BOT_ID,
                    )
                )
            )
        } else {
            val messageStream = channelName.getMessageStream()
            val message =
                pe.nanamochi.banchus.packets.server.MessagePacket(
                    sender = session.username,
                    content = result.message.content,
                    target = packet.target,
                    senderId = session.userId,
                )
            streamService.broadcastMessage(messageStream, message, listOf(session.sessionId))
        }
    }
}
