package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Target
import pe.nanamochi.banchus.domain.error.DomainMessage
import pe.nanamochi.banchus.packets.client.MessagePacket
import pe.nanamochi.banchus.redis.entity.Presence
import pe.nanamochi.banchus.redis.entity.Session

@Service
class ChatService(
    private val channelService: ChannelService,
    private val messageService: MessageService,
    private val streamService: StreamService,
) {
    fun handleIncomingPublicChatMessage(
        packet: MessagePacket,
        session: Session,
    ): Result<Unit, DomainMessage> = binding {
        val channelName = channelService.getChannelName(session, packet.target).bind()
        val target = Target.Channel(channelName)
        val messageStream = channelName.getMessageStream()

        val result = messageService.send(session, target, packet.content).bind()

        val playerMessage =
            pe.nanamochi.banchus.packets.server.MessagePacket(
                sender = session.username,
                content = result.message.content,
                target = packet.target,
                senderId = session.userId,
            )
        streamService.broadcastMessage(messageStream, playerMessage, listOf(session.sessionId))

        if (result.response.isNotBlank()) {
            streamService.broadcastMessage(
                messageStream,
                pe.nanamochi.banchus.packets.server.MessagePacket(
                    sender = Presence.BOT_NAME,
                    content = result.response,
                    target = packet.target,
                    senderId = Presence.BOT_ID,
                ),
            )
        }
    }
}
