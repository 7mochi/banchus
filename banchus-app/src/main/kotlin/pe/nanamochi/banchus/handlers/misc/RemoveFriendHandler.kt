package pe.nanamochi.banchus.handlers.misc

import com.github.michaelbull.result.onFailure
import java.io.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.packets.client.RemoveFriendPacket
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.service.RelationshipService

@Component
@HandleClientPacket(type = PacketType.OSU_REMOVE_FRIEND)
class RemoveFriendHandler(private val relationshipService: RelationshipService) :
    AbstractPacketHandler<RemoveFriendPacket>(PacketType.OSU_ADD_FRIEND) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        packet: RemoveFriendPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        relationshipService.removeFriend(session.userId, packet.userId).onFailure {
            log.warn("User {} failed to remove friend {}: {}", session.userId, packet.userId, it)
        }
    }
}
