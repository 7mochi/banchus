package pe.nanamochi.banchus.handlers.chat

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.MessagePacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.ChatService

@Component
@HandleClientPacket(type = PacketType.OSU_MESSAGE, checkForRestriction = true)
class MessageHandler(private val chatService: ChatService) :
    AbstractPacketHandler<MessagePacket>(PacketType.OSU_MESSAGE) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: MessagePacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        chatService.handleIncomingPublicChatMessage(packet, session, responseStream).onFailure {
            error ->
            log.warn("Message delivery failed for user {}: {}", session.username, error)
        }
    }
}
